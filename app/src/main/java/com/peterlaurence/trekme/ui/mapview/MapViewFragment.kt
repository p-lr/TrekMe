package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.markers.*
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.OrientationEventManager
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.ui.LocationProviderHolder
import com.peterlaurence.trekme.ui.mapview.events.TrackVisibilityChangedEvent
import com.peterlaurence.trekme.viewmodel.common.Location
import com.peterlaurence.trekme.viewmodel.common.LocationProvider
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.makeTileStreamProvider
import com.peterlaurence.trekme.viewmodel.mapview.InMapRecordingViewModel
import com.peterlaurence.trekme.viewmodel.mapview.MapViewViewModel
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.coroutines.CoroutineContext

/**
 * This fragment displays a [Map], using [MapView].
 *
 * @author peterLaurence on 10/02/2019
 */
class MapViewFragment : Fragment(), FrameLayoutMapView.PositionTouchListener, CoroutineScope {
    private lateinit var rootView: FrameLayoutMapView
    private lateinit var mapView: MapView
    private var mMap: Map? = null
    private lateinit var positionMarker: View
    private var lockView = false
    private var requestManageTracksListener: RequestManageTracksListener? = null
    private var requestManageMarkerListener: RequestManageMarkerListener? = null
    private lateinit var locationProvider: LocationProvider
    private var hasCenteredOnFirstLocation = false
    private lateinit var orientationEventManager: OrientationEventManager
    private lateinit var markerLayer: MarkerLayer
    private lateinit var routeLayer: RouteLayer
    private lateinit var distanceLayer: DistanceLayer
    private lateinit var landmarkLayer: LandmarkLayer
    private lateinit var speedListener: SpeedListener
    private lateinit var distanceListener: DistanceLayer.DistanceListener

    private val mapViewViewModel: MapViewViewModel by viewModels()
    private val inMapRecordingViewModel: InMapRecordingViewModel by viewModels()

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val currentMarker: MarkerGson.Marker
        get() = markerLayer.currentMarker

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RequestManageTracksListener && context is RequestManageMarkerListener &&
                context is LocationProviderHolder) {
            requestManageTracksListener = context
            requestManageMarkerListener = context
            locationProvider = context.locationProvider
        } else {
            throw RuntimeException("$context must implement RequestManageTracksListener, " +
                    "RequestManageMarkerListener and LocationProviderHolder")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        mapViewViewModel.getMapLiveData().observe(this, Observer<Map> {
            it?.let {
                onMapChanged(it)
            }
        })

        mapViewViewModel.getCalibrationChangedLiveData().observe(this, Observer<Map> {
            it?.let {
                onSameMapButCalibrationMayChanged(it)
            }
        })

        /**
         * Listen to changes on the live route
         */
        inMapRecordingViewModel.getLiveRoute().observe(
                this, Observer<InMapRecordingViewModel.LiveRoute> {
            it?.let { liveRoute ->
                if (::routeLayer.isInitialized) {
                    routeLayer.updateLiveRoute(liveRoute.route, liveRoute.map)
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        /* Create layout from scratch if it does not exist, else don't re-create the TileView,
         * it handles configuration changes itself
         */
        if (!::rootView.isInitialized) {
            rootView = FrameLayoutMapView(context!!)
            rootView.setPositionTouchListener(this)
        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* Get the speed, distance and orientation indicators from the main layout */
        speedListener = rootView.speedIndicator
        distanceListener = rootView.distanceIndicator
        positionMarker = rootView.positionMarker

        /* Create the instance of the OrientationEventManager */
        if (!::orientationEventManager.isInitialized) {
            orientationEventManager = OrientationEventManager(activity)

            /* Register the position marker as an OrientationListener */
            orientationEventManager.setOrientationListener(positionMarker as OrientationEventManager.OrientationListener?)
            if (savedInstanceState != null) {
                val shouldDisplayOrientation = savedInstanceState.getBoolean(WAS_DISPLAYING_ORIENTATION)
                if (shouldDisplayOrientation) {
                    orientationEventManager.start()
                }
            }
        }

        /* Create the marker layer */
        if (!::markerLayer.isInitialized) {
            markerLayer = MarkerLayer(context)
        }
        markerLayer.setRequestManageMarkerListener(requestManageMarkerListener)

        /* Create the route layer */
        if (!::routeLayer.isInitialized) {
            routeLayer = RouteLayer(this)
        }

        /* Create the distance layer */
        if (!::distanceLayer.isInitialized) {
            distanceLayer = DistanceLayer(context, distanceListener)
        }

        /* Create the landmark layer */
        if (!::landmarkLayer.isInitialized) {
            context?.let {
                landmarkLayer = LandmarkLayer(it, this)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Hide the app title */
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        /* Clear the existing action menu */
        menu.clear()

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_view, menu)

        /* .. and restore some checkable state */
        val item = menu.findItem(R.id.distancemeter_id)
        item.isChecked = distanceLayer.isVisible

        val itemOrientation = menu.findItem(R.id.orientation_enable_id)
        itemOrientation.isChecked = orientationEventManager.isStarted

        val itemLockOnPosition = menu.findItem(R.id.lock_on_position_id)
        itemLockOnPosition.isChecked = lockView

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_marker_id -> {
                markerLayer.addNewMarker()
                return true
            }
            R.id.manage_tracks_id -> {
                requestManageTracksListener?.onRequestManageTracks()
                return true
            }
            R.id.speedometer_id -> {
                speedListener.toggleSpeedVisibility()
                return true
            }
            R.id.distancemeter_id -> {
                distanceListener.toggleDistanceVisibility()
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    distanceLayer.show()
                } else {
                    distanceLayer.hide()
                }
                return true
            }
            R.id.orientation_enable_id -> {
                item.isChecked = orientationEventManager.toggleOrientation()
                return true
            }
            R.id.landmark_id -> {
                landmarkLayer.addNewLandmark()
                return true
            }
            R.id.lock_on_position_id -> {
                item.isChecked = !item.isChecked
                lockView = item.isChecked
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPositionTouch() {
        if (::mapView.isInitialized) {
            mapView.scale = 1f
        }
        centerOnPosition()
    }

    override fun onStart() {
        super.onStart()
        job = Job()
        EventBus.getDefault().register(this)

        mapViewViewModel.updateMapIfNecessary(mMap)
    }

    override fun onResume() {
        super.onResume()

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationProvider.start {
            onLocationReceived(it)
        }
    }

    private fun stopLocationUpdates() {
        locationProvider.stop()
    }

    override fun onPause() {
        super.onPause()

        /* Save battery */
        stopLocationUpdates()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            speedListener.hideSpeed()
            distanceLayer.hide()
            orientationEventManager.stop()
        } else {
            mapViewViewModel.updateMapIfNecessary(mMap)
        }
    }

    @Subscribe
    fun onTrackVisibilityChangedEvent(event: TrackVisibilityChangedEvent) {
        routeLayer.onTrackVisibilityChanged()
    }

    @Subscribe
    fun onTrackChangedEvent(event: TrackImporter.GpxParseResult) {
        routeLayer.onTrackChanged(event.map, event.routes)
        if (event.newMarkersCount > 0) {
            markerLayer.onMapMarkerUpdate()
        }
    }

    /**
     * The view model reported that map is still the same. But the calibration may have changed.
     * Only the fragment can do this check because the difference between the old and new value is
     * based on the state of an internal object of [MapView].
     */
    private fun onSameMapButCalibrationMayChanged(map: Map) {
        if (::mapView.isInitialized) {
            val newBounds = map.mapBounds
            val c = mapView.coordinateTranslater
            if (newBounds != null && !newBounds.compareTo(c.left, c.top, c.right, c.bottom)) {
                setMapViewBounds(mapView, map)
            }
        }
    }

    /**
     * Once the map is updated, a [MapView] instance is created, so layers can be
     * updated.
     */
    private fun onMapChanged(map: Map) {
        hasCenteredOnFirstLocation = false
        setMap(map)
        inMapRecordingViewModel.reload()
        updateLayers()
    }

    private fun updateLayers() {
        mMap?.let { map ->
            /* Update the marker layer */
            markerLayer.init(map, mapView)

            /* Update the route layer */
            routeLayer.init(map, mapView)

            /* Update the distance layer */
            distanceLayer.init(map, mapView)

            /* Update the landmark layer */
            landmarkLayer.init(map, mapView)
        }
    }

    override fun onStop() {
        job.cancel()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        MapLoader.clearMapMarkerUpdateListener()
    }

    override fun onDetach() {
        super.onDetach()

        requestManageTracksListener = null
        requestManageMarkerListener = null
        orientationEventManager.stop()
    }

    private fun onLocationReceived(location: Location) {
        if (isHidden) return

        /* If there is no MapView, no need to go further */
        if (!::mapView.isInitialized) {
            return
        }

        /* In the case there is no Projection defined, the latitude and longitude are used */
        val projection = mMap!!.projection
        if (projection != null) {
            launch {
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
        if (::speedListener.isInitialized) {
            speedListener.onSpeed(location.speed, SpeedUnit.KM_H)
        }
    }

    fun currentMarkerEdited() {
        markerLayer.updateCurrentMarker()
    }

    /**
     * Actions taken when the position changes:
     * * Update the position on the [Map]
     * * Update the landmarks
     * * If we locked the view, we center the TileView on the current position only if the position
     * is inside the map.
     *
     * @param x the projected X coordinate, or longitude if there is no [Projection]
     * @param y the projected Y coordinate, or latitude if there is no [Projection]
     */
    private fun updatePosition(x: Double, y: Double) {
        mapView.moveMarker(positionMarker, x, y)
        landmarkLayer.onPositionUpdate(x, y)

        if (lockView || !hasCenteredOnFirstLocation) {
            hasCenteredOnFirstLocation = true
            if (mMap?.containsLocation(x, y) == true) {
                centerOnPosition()
            }
        }
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
        mapView.id = R.id.tileview_id
        mapView.isSaveEnabled = true
        rootView.addView(mapView, 0)

        /* The MapView can have only one MarkerTapListener.
         * It dispatches the tap event to child layers.
         */
        mapView.setMarkerTapListener(object : MarkerTapListener {
            override fun onMarkerTap(view: View, x: Int, y: Int) {
                markerLayer.onMarkerTap(view, x, y)
                landmarkLayer.onMarkerTap(view, x, y)
            }
        })
    }

    private fun removeCurrentMapView() {
        try {
            mapView.destroy()
            rootView.removeView(mapView)
        } catch (e: Exception) {
            // don't care
        }
    }

    /**
     * Sets the map to generate a new [MapView].
     *
     * @param map The new [Map] object
     */
    private fun setMap(map: Map) {
        mMap = map
        val mapView = MapView(this.context!!)
        val tileSize = map.levelList.firstOrNull()?.tile_size?.x ?: return

        val config = MapViewConfiguration(map.levelList.size, map.widthPx, map.heightPx, tileSize,
                makeTileStreamProvider(map)).setMaxScale(2f).setMagnifyingFactor(1).setPadding(tileSize * 2)

        /* The MapView only supports one square tile size */
        mapView.configure(config)

        /* Map calibration */
        setMapViewBounds(mapView, map)

        /* The position + orientation reticule */
        try {
            val parent = positionMarker.parent as ViewGroup
            parent.removeView(positionMarker)
        } catch (e: Exception) {
            // don't care
        }

        mapView.addMarker(positionMarker, 0.0, 0.0, -0.5f, -0.5f)

        /* Remove the existing MapView, then add the new one */
        removeCurrentMapView()
        setMapView(mapView)
    }

    private fun centerOnPosition() {
        if (::mapView.isInitialized) {
            mapView.moveToMarker(positionMarker, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(WAS_DISPLAYING_ORIENTATION, orientationEventManager.isStarted)
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

    enum class SpeedUnit {
        KM_H, MPH
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface RequestManageTracksListener {
        fun onRequestManageTracks()
    }

    /**
     * Same as [RequestManageTracksListener].
     */
    interface RequestManageMarkerListener {
        fun onRequestManageMarker(marker: MarkerGson.Marker)
    }

    /**
     * As the `MapViewFragment` receives speed data, it can dispatch speed
     * information to other sub-components.
     */
    interface SpeedListener {
        /**
         * @param speed speed in meters per second
         * @param unit the desired unit to use for display
         */
        fun onSpeed(speed: Float, unit: SpeedUnit)

        fun toggleSpeedVisibility()

        fun hideSpeed()
    }

    companion object {
        const val TAG = "MapViewFragment"
        private const val WAS_DISPLAYING_ORIENTATION = "wasDisplayingOrientation"
    }
}
