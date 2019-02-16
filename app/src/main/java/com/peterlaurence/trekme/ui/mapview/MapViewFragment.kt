package com.peterlaurence.trekme.ui.mapview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.OrientationEventManager
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.ProjectionTask
import com.peterlaurence.trekme.model.map.MapProvider
import com.peterlaurence.trekme.ui.mapview.events.TrackChangedEvent
import com.peterlaurence.trekme.ui.mapview.events.TrackVisibilityChangedEvent
import com.qozix.tileview.TileView
import com.qozix.tileview.widgets.ZoomPanLayout

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * A [Fragment] subclass that implements required interfaces to be used with a
 * [GoogleApiClient].
 *
 * Activities that contain this fragment must implement the
 * [RequestManageTracksListener] and [MapProvider] interfaces to handle
 * interaction events.
 *
 * @author peterLaurence
 */
class MapViewFragment : Fragment(), ProjectionTask.ProjectionUpdateLister,
        FrameLayoutMapView.PositionTouchListener,
        FrameLayoutMapView.LockViewListener,
        CoroutineScope {
    private lateinit var rootView: FrameLayoutMapView
    private lateinit var mTileView: TileViewExtended
    private var mMap: Map? = null
    private lateinit var positionMarker: View
    private var lockView = false
    private var requestManageTracksListener: RequestManageTracksListener? = null
    private var requestManageMarkerListener: RequestManageMarkerListener? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var orientationEventManager: OrientationEventManager
    private lateinit var markerLayer: MarkerLayer
    private lateinit var routeLayer: RouteLayer
    private lateinit var distanceLayer: DistanceLayer
    private lateinit var speedListener: SpeedListener
    private lateinit var distanceListener: DistanceLayer.DistanceListener
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    val currentMarker: MarkerGson.Marker
        get() = markerLayer.currentMarker

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RequestManageTracksListener && context is RequestManageMarkerListener) {
            requestManageTracksListener = context
            requestManageMarkerListener = context
        } else {
            throw RuntimeException("$context must implement RequestManageTracksListener, MapProvider and LocationProvider")
        }

        /* The Google api client is re-created here as the onAttach method will always be called for
         * a retained fragment.
         */
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.activity!!.applicationContext)
        locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 1000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                for (location in locationResult!!.locations) {
                    onLocationReceived(location)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)

        /* The location request specific to this fragment */
        if (!::locationRequest.isInitialized) {
            locationRequest = LocationRequest()
            locationRequest.interval = 1000
            locationRequest.fastestInterval = 1000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        /* Create layout from scratch if it does not exist, else don't re-create the TileView,
         * it handles configuration changes itself
         */
        if (!::rootView.isInitialized) {
            rootView = FrameLayoutMapView(context)
            rootView.setPositionTouchListener(this)
            rootView.setLockViewListener(this)
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
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onPositionTouch() {
        if (::mTileView.isInitialized) {
            mTileView.scale = 1f
        }
        centerOnPosition()
    }

    override fun onLockView(lock: Boolean) {
        lockView = lock
    }

    override fun onStart() {
        super.onStart()
        job = Job()
        EventBus.getDefault().register(this)

        updateMapIfNecessary()
    }

    override fun onResume() {
        super.onResume()

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, null)
        }
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
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
            updateMapIfNecessary()
        }
    }

    @Subscribe
    fun onTrackVisibilityChangedEvent(event: TrackVisibilityChangedEvent) {
        routeLayer.onTrackVisibilityChanged()
    }

    @Subscribe
    fun onTrackChangedEvent(event: TrackChangedEvent) {
        routeLayer.onTrackChanged(event.map, event.routeList)
        if (event.addedMarkers > 0) {
            markerLayer.onMapMarkerUpdate()
        }
    }

    /**
     * Only update the map if its a new one. <br></br>
     * Once the map is updated, a [TileViewExtended] instance is created, so layers can be
     * updated.
     */
    private fun updateMapIfNecessary() {
        val map = MapProvider.getCurrentMap()
        if (map != null) {
            if (mMap != null && mMap!!.equals(map)) {
                val newBounds = map.mapBounds

                if (::mTileView.isInitialized) {
                    val c = mTileView.coordinateTranslater
                    if (newBounds != null && !newBounds.compareTo(c.left, c.top, c.right, c.bottom)) {
                        setTileViewBounds(mTileView, map)
                    }
                }
            } else {
                setMap(map)
                updateLayers()
            }
        }
    }

    private fun updateLayers() {
        /* Update the marker layer */
        markerLayer.init(mMap, mTileView)

        /* Update the route layer */
        routeLayer.init(mMap!!, mTileView)

        /* Update the distance layer */
        distanceLayer.init(mMap, mTileView)
    }

    override fun onStop() {
        job.cancel()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        MapLoader.clearMapMarkerUpdateListener()
        MapLoader.clearMapRouteUpdateListener()
    }

    override fun onDetach() {
        super.onDetach()

        requestManageTracksListener = null
        requestManageMarkerListener = null
        orientationEventManager.stop()
    }

    private fun onLocationReceived(location: Location?) {
        if (isHidden) return

        if (location != null) {
            /* If there is no TileView, no need to go further */
            if (!::mTileView.isInitialized) {
                return
            }

            /* In the case there is no Projection defined, the latitude and longitude are used */
            val projection = mMap!!.projection
            if (projection != null) {
                val projectionTask = ProjectionTask(this, location.latitude,
                        location.longitude, projection)
                projectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            } else {
                updatePosition(location.longitude, location.latitude)
            }

            /* If the user wants to see the speed */
            if (::speedListener.isInitialized) {
                speedListener.onSpeed(location.speed, SpeedUnit.KM_H)
            }
        }
    }

    override fun onProjectionUpdate(projectedValues: DoubleArray) {
        updatePosition(projectedValues[0], projectedValues[1])
    }

    fun currentMarkerEdited() {
        markerLayer.updateCurrentMarker()
    }

    /**
     * Updates the position on the [Map].
     * Also, if we locked the view, we center the TileView on the current position.
     *
     * @param x the projected X coordinate, or longitude if there is no [Projection]
     * @param y the projected Y coordinate, or latitude if there is no [Projection]
     */
    private fun updatePosition(x: Double, y: Double) {
        mTileView.moveMarker(positionMarker, x, y)

        if (lockView) {
            centerOnPosition()
        }
    }

    private fun setTileView(tileView: TileViewExtended) {
        mTileView = tileView
        mTileView.id = R.id.tileview_id
        mTileView.isSaveEnabled = true
        rootView.addView(mTileView, 0)
        mTileView.setSingleTapListener(rootView)
        mTileView.setScrollListener(rootView)
    }

    private fun removeCurrentTileView() {
        try {
            mTileView.destroy()
            rootView.removeView(mTileView)
        } catch (e: Exception) {
            // don't care
        }

    }

    /**
     * Sets the map to generate a new [TileViewExtended].
     *
     * @param map The new [Map] object
     */
    private fun setMap(map: Map) {
        mMap = map
        val tileView = TileViewExtended(this.context)

        /* Set the size of the view in px at scale 1 */
        tileView.setSize(map.widthPx, map.heightPx)

        /* Lowest scale */
        val levelList = map.levelList
        val minScale = 1 / Math.pow(2.0, (levelList.size - 1).toDouble()).toFloat()

        /* Scale limits */
        tileView.setScaleLimits(minScale, 2f)

        /* Starting scale */
        tileView.scale = minScale

        /* DetailLevel definition */
        for (level in levelList) {
            /* Calculate each level scale for best precision */
            val scale = 1 / Math.pow(2.0, (levelList.size - level.level - 1).toDouble()).toFloat()

            tileView.addDetailLevel(scale, level.level, level.tile_size.x, level.tile_size.y)
        }

        /* Allow the scale to be no less to see the entire map */
        tileView.setMinimumScaleMode(ZoomPanLayout.MinimumScaleMode.FIT)

        /* Render while panning */
        tileView.setShouldRenderWhilePanning(true)

        /* Map calibration */
        setTileViewBounds(tileView, map)

        /* The BitmapProvider */
        tileView.setBitmapProvider(map.bitmapProvider)

        /* The position + orientation reticule */
        try {
            val parent = positionMarker.parent as ViewGroup
            parent.removeView(positionMarker)
        } catch (e: Exception) {
            // don't care
        }

        tileView.addMarker(positionMarker, 0.0, 0.0, -0.5f, -0.5f)

        /* Remove the existing TileView, then add the new one */
        removeCurrentTileView()
        setTileView(tileView)
    }

    private fun centerOnPosition() {
        if (::mTileView.isInitialized) {
            mTileView.moveToMarker(positionMarker, true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(WAS_DISPLAYING_ORIENTATION, orientationEventManager.isStarted)
    }

    private fun setTileViewBounds(tileView: TileView, map: Map) {
        val mapBounds = map.mapBounds
        if (mapBounds != null) {
            tileView.defineBounds(mapBounds.X0,
                    mapBounds.Y0,
                    mapBounds.X1,
                    mapBounds.Y1)
        } else {
            tileView.defineBounds(0.0, 0.0, 1.0, 1.0)
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
     * As the `MapViewFragment` is a [LocationListener], it can dispatch speed
     * information to other sub-components.
     */
    interface SpeedListener {
        fun onSpeed(speed: Float, unit: SpeedUnit)

        fun toggleSpeedVisibility()

        fun hideSpeed()
    }

    companion object {
        const val TAG = "MapViewFragment"
        private const val WAS_DISPLAYING_ORIENTATION = "wasDisplayingOrientation"
    }
}
