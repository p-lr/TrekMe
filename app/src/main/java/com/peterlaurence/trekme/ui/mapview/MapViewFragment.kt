package com.peterlaurence.trekme.ui.mapview

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.location.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.data.orientation.OrientationSensor
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.ui.mapview.components.CompassView
import com.peterlaurence.trekme.ui.mapview.components.PositionOrientationMarker
import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
import com.peterlaurence.trekme.util.collectWhileResumed
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.makeMapViewTileStreamProvider
import com.peterlaurence.trekme.viewmodel.mapview.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener
import ovh.plrapps.mapview.api.*
import ovh.plrapps.mapview.markers.MarkerTapListener
import javax.inject.Inject

/**
 * This fragment displays a [Map], using [MapView].
 *
 * @author P.Laurence on 10/02/2019
 */
@AndroidEntryPoint
class MapViewFragment : Fragment(), MapViewFragmentPresenter.PositionTouchListener,
        ReferentialListener {

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var mapViewEventBus: MapViewEventBus

    @Inject
    lateinit var mapLoader: MapLoader

    @Inject
    lateinit var routeRepository: RouteRepository

    @Inject
    lateinit var locationSource: LocationSource

    private var presenter: MapViewFragmentContract.Presenter? = null
    private var mapView: MapView? = null
    private var mMap: Map? = null
    private var positionMarker: PositionOrientationMarker? = null
    private var compassView: CompassView? = null
    private var lockView = false

    /* Map settings */
    private var lastRotationMode: RotationMode? = null
    private var shouldCenterOnFirstLocation = false

    private var orientationSensor: OrientationSensor? = null
    private var markerLayer: MarkerLayer? = null
    private var routeLayer: RouteLayer? = null
    private var distanceLayer: DistanceLayer? = null
    private var landmarkLayer: LandmarkLayer? = null
    private var speedListener: SpeedListener? = null
    private var distanceListener: DistanceLayer.DistanceListener? = null
    private var orientationJob: Job? = null

    private val mapViewViewModel: MapViewViewModel by viewModels()
    private val inMapRecordingViewModel: InMapRecordingViewModel by viewModels()

    private var state: Bundle? = null

    private var referentialData: ReferentialData = ReferentialData(false, 0f, 1f, 0.0, 0.0)
        set(value) {
            field = value
            positionMarker?.onReferentialChanged(value)
            compassView?.referentialData = value
            rememberScaleRatio()
        }

    override fun onReferentialChanged(refData: ReferentialData) {
        referentialData = refData
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        /* When the fragment is created for the first time, center on first location */
        shouldCenterOnFirstLocation = savedInstanceState == null

        /* Observe location changes */
        locationSource.locationFlow.collectWhileResumed(this) {
            onLocationReceived(it)
        }

        appEventBus.gpxImportEvent.collectWhileResumed(this) {
            onGpxImportEvent(it)
        }

        mapViewEventBus.trackVisibilityChangedSignal.collectWhileResumed(this) {
            routeLayer?.onTrackVisibilityChanged()
        }

        mapViewViewModel.getLicenseFlow().collectWhileResumed(this) {
            onIgnLicenseEvent(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val context = context ?: return null

        /* The navigation framework seems to not save the state before re-creating the view.. so
         * we do this by hand. */
        val mergedState = savedInstanceState ?: state

        /* Create the presenter */
        presenter = MapViewFragmentPresenter(inflater, container, context)
        val presenter = presenter ?: return null
        presenter.setPositionTouchListener(this)

        /* Get the speed, distance and orientation indicators from the view */
        speedListener = presenter.view.speedIndicator
        distanceListener = presenter.view.distanceIndicator
        positionMarker = presenter.view.positionMarker
        compassView = presenter.view.compassView

        compassView?.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                if (getRotationMode() == RotationMode.FREE) {
                    animateMapViewToNorth()
                }
            }
        }

        /* Create the instance of the OrientationSensor */
        orientationSensor = OrientationSensor(requireActivity())

        /* Create the marker layer */
        markerLayer = MarkerLayer(mapLoader, lifecycleScope)

        /* Create the route layer, restoring the previous state (if any) */
        val routeLayerState = mergedState?.getParcelable<RouteLayerState>(ROUTE_LAYER_STATE)
        routeLayer = RouteLayer(lifecycleScope, routeLayerState, routeRepository)

        /* Create the distance layer */
        distanceLayer = DistanceLayer(context, distanceListener)

        /* Create the landmark layer */
        landmarkLayer = LandmarkLayer(context, lifecycleScope, mapLoader)

        /* Create the MapView.
         * Attached views must be known before onCreateView returns, or the save/restore state
         * won't work.
         */
        mapView = MapView(context).also {
            it.apply {
                id = R.id.tileview_id
                isSaveEnabled = true
            }
            presenter.setMapView(it)
        }

        lifecycleScope.launch {
            /* Apply the Map to the current MapView */
            getAndApplyMap()

            /* Eventually restore the distance layer if it was visible before device rotation */
            val distanceLayerState = mergedState?.getParcelable<DistanceLayer.State>(DISTANCE_LAYER_STATE)
            if (distanceLayerState != null && distanceLayerState.visible) {
                presenter.view.distanceIndicator.showDistance()
                distanceLayer?.show(distanceLayerState)
            }

            /* Restore speed visibility */
            speedListener?.setSpeedVisible(mapViewViewModel.getSpeedVisibility().first())

            /* Restore orientation visibility */
            mapViewViewModel.getOrientationVisibility().first().also { visible ->
                orientationSensor?.takeIf { visible }?.apply {
                    start()
                    onOrientationSensorChanged()
                }
            }

            /* Restore GPS data visibility */
            presenter.setGpsDataVisible(mapViewViewModel.getGpsDataVisibility().first())

            /* In free-rotating mode, show the compass right from the start */
            if (getRotationMode() == RotationMode.FREE) {
                compassView?.visibility = View.VISIBLE
            } else {
                compassView?.visibility = View.GONE
            }
        }

        /**
         * Using the navigation framework, the state of the MapView is automatically restored when
         * we navigate back to this fragment. However, the rotation mode might change from e.g
         * [RotationMode.FREE] to [RotationMode.NONE]. In this case, the MapView sees its previous
         * angle restored even if the new rotation mode doesn't permit it. As a workaround, we
         * enforce an angle of 0 after the fragment is started (if we attempt to do this earlier in
         * the life-cycle, it's overridden by the angle restore mechanism of MapView). */
        lifecycleScope.launchWhenStarted {
            if (mergedState != null) {
                if (mergedState.getBoolean(WAS_ROTATED) && getRotationMode() == RotationMode.NONE) {
                    mapView?.disableRotation(0f)
                }
            }
        }

        return presenter.androidView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        orientationSensor?.stop()
        orientationJob?.cancel()

        destroyLayers()
        positionMarker = null
        compassView = null
        distanceListener = null
        speedListener = null
        presenter = null

        mapView?.destroy()
        mapView = null
    }

    private suspend fun getRotationMode(): RotationMode {
        val rotMode =  mapViewViewModel.getRotationMode().first()
        lastRotationMode = rotMode
        return rotMode
    }

    /**
     * When the [OrientationSensor] is started or stopped, this function should be called.
     */
    private fun onOrientationSensorChanged() = lifecycleScope.launchWhenResumed {
        val orientationSensor = orientationSensor ?: return@launchWhenResumed
        mapViewViewModel.setOrientationVisibility(orientationSensor.isStarted)

        if (orientationSensor.isStarted) {
            positionMarker?.onOrientationEnable()
            if (getRotationMode() != RotationMode.NONE) {
                compassView?.visibility = View.VISIBLE
            }

            orientationJob = lifecycleScope.launch {
                orientationSensor.getAzimuthFlow().collect { azimuth ->
                    positionMarker?.onOrientation(azimuth)
                    if (getRotationMode() == RotationMode.FOLLOW_ORIENTATION) {
                        mapView?.setAngle(-azimuth)
                    }
                }
            }
        } else {
            positionMarker?.onOrientationDisable()
            orientationJob?.cancel()
            /* Set the MapView like it was before: North-oriented */
            orientationJob?.invokeOnCompletion {
                lifecycleScope.launchWhenResumed {
                    if (getRotationMode() == RotationMode.FOLLOW_ORIENTATION) {
                        animateMapViewToNorth()
                        compassView?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun animateMapViewToNorth() {
        /* Wrapper class, necessary for the the animator to work (which uses reflection to infer
         * method names..) */
        @Suppress("unused")
        val wrapper = object {
            fun setAngle(angle: Float) {
                mapView?.setAngle(angle)
            }

            fun getAngle(): Float {
                return referentialData.angle
            }
        }
        ObjectAnimator.ofFloat(wrapper, "angle", if (referentialData.angle > 180f) 360f else 0f).apply {
            interpolator = DecelerateInterpolator()
            duration = 800
            start()
        }
    }

    private suspend fun getAndApplyMap() {
        val map = mapViewViewModel.getMap()
        if (map != null) applyMap(map)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Clear the existing action menu */
        menu.clear()

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_view, menu)

        /* .. and restore some checkable state */
        val itemSpeed = menu.findItem(R.id.speedometer_id)
        lifecycleScope.launch {
            itemSpeed.isChecked = mapViewViewModel.getSpeedVisibility().first()
        }

        val itemDistance = menu.findItem(R.id.distancemeter_id)
        itemDistance.isChecked = distanceLayer?.isVisible ?: false

        val itemDistanceOnTrack = menu.findItem(R.id.distance_on_track_id)
        itemDistanceOnTrack.isChecked = routeLayer?.isDistanceOnTrackActive ?: false

        val itemOrientation = menu.findItem(R.id.orientation_enable_id)
        lifecycleScope.launch {
            itemOrientation.isChecked = mapViewViewModel.getOrientationVisibility().first()
        }

        val itemLockOnPosition = menu.findItem(R.id.lock_on_position_id)
        itemLockOnPosition.isChecked = lockView

        val itemGpsData = menu.findItem(R.id.gps_data_enable_id)
        lifecycleScope.launch {
            itemGpsData.isChecked = mapViewViewModel.getGpsDataVisibility().first()
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_marker_id -> {
                markerLayer?.addNewMarker()
                return true
            }
            R.id.manage_tracks_id -> {
                findNavController().navigate(R.id.action_mapViewFragment_to_tracksManageFragment)
                return true
            }
            R.id.speedometer_id -> {
                speedListener?.toggleSpeedVisibility()?.also { visible ->
                    mapViewViewModel.setSpeedVisibility(visible)
                }
                item.isChecked = !item.isChecked
                return true
            }
            R.id.distancemeter_id -> {
                distanceListener?.toggleDistanceVisibility()
                item.isChecked = !item.isChecked
                distanceLayer?.toggle()
                return true
            }
            R.id.distance_on_track_id -> {
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    routeLayer?.activateDistanceOnTrack()
                } else {
                    routeLayer?.disableDistanceOnTrack()
                }
                return true
            }
            R.id.orientation_enable_id -> {
                item.isChecked = orientationSensor?.toggle() ?: false
                onOrientationSensorChanged()
                return true
            }
            R.id.landmark_id -> {
                landmarkLayer?.addNewLandmark()
                return true
            }
            R.id.lock_on_position_id -> {
                item.isChecked = !item.isChecked
                lockView = item.isChecked
                return true
            }
            R.id.gps_data_enable_id -> {
                lifecycleScope.launchWhenResumed {
                    val newVisi = mapViewViewModel.toggleGpsDataVisibility()
                    item.isChecked = newVisi
                    presenter?.setGpsDataVisible(newVisi)
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPositionTouch() {
        centerOnPosition()
    }

    /**
     * Save the state in [onStop].
     * There are two different scenarios where we need to save the state:
     *
     * 1. Device is rotated. In this case, the fragment will be **destroyed**, so all variables like
     * [state] will be null inside [onCreateView]. However, before the fragment is destroyed,
     * [onStop] then [onSaveInstanceState] are invoked. [onSaveInstanceState] is our only chance to
     * save some state which will be passed as bundle inside the next [onCreateView]. Since [onStop]
     * is invoked before [onSaveInstanceState], it's ok to save the state there and take it into
     * account in [onSaveInstanceState].
     *
     * 2. User navigates away from this fragment using e.g the main menu > Settings. In this case,
     * the fragment is **not destroyed**, which is why [onSaveInstanceState] is **not** invoked.
     * However, [onStop] is invoked (before [onDestroyView]). So saving the state here enables us
     * to access it in the next [onCreateView], because [state] is then non-null while the bundle
     * passed as argument of [onCreateView] is null (the fragment wasn't destroyed).
     *
     * In other words, for future reference (it's easy to forget), [onSaveInstanceState] is only
     * involved when the fragment is about to be destroyed, **but** it's not the only scenario where
     * we need to save the state. Sometimes, only the view of fragment is destroyed, not the fragment
     * itself.
     */
    override fun onStop() {
        super.onStop()
        state = saveState()
    }

    private fun onGpxImportEvent(event: TrackImporter.GpxImportResult) {
        if (event is TrackImporter.GpxImportResult.GpxImportOk) {
            routeLayer?.onTrackChanged(event.map, event.routes)
            if (event.newMarkersCount > 0) {
                markerLayer?.updateMarkers()
            }
        }
    }

    private fun onIgnLicenseEvent(event: LicenseEvent) = when (event) {
        is FreeLicense, ValidIgnLicense -> { /* Nothing to do */ }
        is ErrorIgnLicenseEvent -> {
            clearMap()
            presenter?.showMessage(getString(R.string.missing_ign_license))
        }
    }

    /**
     * Once we get a [Map], a [MapView] instance is created, so layers can be
     * updated.
     */
    private suspend fun applyMap(map: Map) {
        mapView?.also {
            configureMapView(it, map)
        }
        initLayers()

        /**
         * Listen to changes on the live route.
         * It's called only now because the [RouteLayer] must be first initialized.
         */
        inMapRecordingViewModel.getLiveRoute().observe(viewLifecycleOwner) {
            it?.let { liveRoute ->
                routeLayer?.drawLiveRoute(liveRoute)
            }
        }
    }

    private fun initLayers() {
        mMap?.let { map ->
            val mapView = mapView ?: return

            /* Update the marker layer */
            markerLayer?.init(map, mapView)

            /* Update the route layer */
            routeLayer?.init(map, mapView)

            /* Update the distance layer */
            distanceLayer?.init(map, mapView)

            /* Update the landmark layer */
            landmarkLayer?.init(map, mapView)
        }
    }

    private fun destroyLayers() {
        distanceLayer?.destroy()
        distanceLayer = null
        landmarkLayer?.destroy()
        landmarkLayer = null
        markerLayer = null
        routeLayer?.destroy()
        routeLayer = null
    }

    override fun onDetach() {
        super.onDetach()
        orientationSensor?.stop()
    }

    private fun onLocationReceived(location: Location) {
        presenter?.setGpsData(location)

        /* If there is no MapView, no need to go further */
        mapView ?: return

        /* If the map isn't configured (yet), no need to go further */
        val map = mMap ?: return
        /* In the case there is no Projection defined, the latitude and longitude are used */
        val projection = map.projection
        if (projection != null) {
            lifecycleScope.launch {
                val projectedValues = withContext(Dispatchers.Default) {
                    projection.doProjection(location.latitude, location.longitude)
                }
                if (projectedValues != null) {
                    updatePosition(projectedValues[0], projectedValues[1])
                }
            }
        } else {
            updatePosition(location.longitude, location.latitude)
        }

        /* If the user wants to see the speed */
        location.speed?.run {
            speedListener?.onSpeed(this)
        }
    }

    /**
     * Actions taken when the position changes:
     * * Update the position on the [Map]
     * * Update the landmarks
     * * If we locked the view or if the fragment has just been created, we center the TileView on
     *   the current position only if the position is inside the map.
     *
     * @param x the projected X coordinate, or longitude if there is no [Projection]
     * @param y the projected Y coordinate, or latitude if there is no [Projection]
     */
    private fun updatePosition(x: Double, y: Double) {
        positionMarker?.also {
            mapView?.moveMarker(it, x, y)
        }
        landmarkLayer?.onPositionUpdate(x, y)

        if (lockView || shouldCenterOnFirstLocation) {
            shouldCenterOnFirstLocation = false
            if (mMap?.containsLocation(x, y) == true) {
                centerOnPosition()
            }
        }
    }

    private fun rememberScaleRatio() = lifecycleScope.launch {
        val mapView = mapView ?: return@launch
        val currentScaleRatio = mapView.scale * 100f / mapViewViewModel.getMaxScale().first()
        mapViewEventBus.rememberMapState(currentScaleRatio.toInt())
    }

    private fun removeCurrentMapView() {
        mapView?.destroy()
        presenter?.removeMapView(mapView)
    }

    /**
     * Cleanup internal state and the [MapView].
     */
    private fun clearMap() {
        mMap = null
        removeCurrentMapView()
    }

    /**
     * Sets the map to configure the [MapView].
     *
     * @param map The new [Map] object
     */
    private suspend fun configureMapView(mapView: MapView, map: Map) {
        mMap = map
        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: return

        val tileStreamProvider = makeMapViewTileStreamProvider(map)
        if (tileStreamProvider == null) {
            presenter?.showMessage(requireContext().getString(R.string.unknown_map_source))
            return
        }
        val magnifyingFactor = mapViewViewModel.getMagnifyingFactor().first()
        val maxScale = mapViewViewModel.getMaxScale().first()
        val config = MapViewConfiguration(
                map.levelList.size, map.widthPx, map.heightPx, tileSize, tileStreamProvider)
                .setMaxScale(maxScale)
                .setMagnifyingFactor(magnifyingFactor)
                .setPadding(tileSize)

        /* The MapView only supports one square tile size */
        mapView.configure(config)

        /* Map calibration */
        setMapViewBounds(mapView, map)

        /* The position + orientation reticule */
        try {
            val parent = positionMarker?.parent as ViewGroup
            parent.removeView(positionMarker)
        } catch (e: Exception) {
            // don't care
        }

        positionMarker?.also {
            mapView.addMarker(it, 0.0, 0.0, -0.5f, -0.5f)
        }

        /* The MapView can have only one MarkerTapListener.
         * It dispatches the tap event to child layers.
         */
        mapView.setMarkerTapListener(object : MarkerTapListener {
            override fun onMarkerTap(view: View, x: Int, y: Int) {
                markerLayer?.onMarkerTap(view, x, y)
                landmarkLayer?.onMarkerTap(view, x, y)
            }
        })

        when (getRotationMode()) {
            RotationMode.FREE -> mapView.enableRotation(true)
            RotationMode.FOLLOW_ORIENTATION -> mapView.enableRotation(false)
            RotationMode.NONE -> {
            } // nothing to do
        }

        mapView.addReferentialListener(this)
    }

    private fun centerOnPosition() = lifecycleScope.launchWhenResumed {
        val positionMarker = positionMarker ?: return@launchWhenResumed
        val scaleCentered = mapViewViewModel.getScaleCentered().first()
        val defineScaleCentered = mapViewViewModel.getDefineScaleCentered().first()
        if (defineScaleCentered) {
            mapView?.moveToMarker(positionMarker, scaleCentered, true)
        } else {
            mapView?.moveToMarker(positionMarker, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if (state != null) {
            outState.putAll(state)
        }
    }

    private fun saveState(): Bundle {
        val bundle = Bundle()
        distanceLayer?.also {
            bundle.putParcelable(DISTANCE_LAYER_STATE, it.state)
        }
        routeLayer?.also {
            bundle.putParcelable(ROUTE_LAYER_STATE, it.getState())
        }
        bundle.putBoolean(WAS_ROTATED, RotationMode.NONE == lastRotationMode)
        return bundle
    }

    private fun setMapViewBounds(mapView: MapView, map: Map) {
        val mapBounds = map.mapBounds
        if (mapBounds != null) {
            mapView.defineBounds(mapBounds.X0,
                    mapBounds.Y0,
                    mapBounds.X1,
                    mapBounds.Y1)
        } else {
            mapView.defineBounds(0.0, 0.0, 1.0, 1.0)
        }
    }

    /**
     * As this fragment receives speed data, it can dispatch speed
     * information to other sub-components.
     */
    interface SpeedListener {
        /**
         * @param speed speed in meters per second
         */
        fun onSpeed(speed: Float)

        fun setSpeedVisible(v: Boolean)

        fun toggleSpeedVisibility(): Boolean

        fun hideSpeed()
    }

    companion object {
        const val TAG = "MapViewFragment"
        private const val DISTANCE_LAYER_STATE = "distanceLayerState"
        private const val ROUTE_LAYER_STATE = "routeLayerState"
        private const val WAS_ROTATED = "wasRotated"
    }
}

interface MapViewFragmentContract {
    interface Presenter {
        val androidView: android.view.View
        val view: View
        fun showMessage(msg: String)
        fun setPositionTouchListener(listener: MapViewFragmentPresenter.PositionTouchListener)
        fun setMapView(mapView: MapView)
        fun removeMapView(mapView: MapView?)
        fun setGpsData(location: Location)
        fun setGpsDataVisible(visible: Boolean)
    }

    interface View {
        val speedIndicator: MapViewFragment.SpeedListener
        val distanceIndicator: DistanceLayer.DistanceListener
        val positionMarker: PositionOrientationMarker
        val compassView: CompassView
    }
}