package com.peterlaurence.trekme.ui.mapview

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.mapview.paths.PathView
import com.peterlaurence.mapview.paths.addPathView
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getRoutesForMap
import com.peterlaurence.trekme.core.map.route.MarkerIndexed
import com.peterlaurence.trekme.core.map.route.NearestMarkerCalculator
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * All [RouteGson.Route] are managed here.
 * This object is intended to be used exclusively by the [MapViewFragment].
 *
 * After being created, the method [init] has to be called.
 *
 * @author peterLaurence on 13/05/17 -- Converted to Kotlin on 16/02/2019
 */
class RouteLayer(private val coroutineScope: CoroutineScope) :
        TracksManageFragment.TrackChangeListener,
        CoroutineScope by coroutineScope {
    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var pathView: PathView
    private val liveRouteView: PathView by lazy {
        val context = mapView.context
        val view = PathView(context)
        mapView.addPathView(view)
        view
    }
    private val processedStaticRoutes = mutableListOf<RouteGson.Route>()
    private val processedLiveRoute = mutableListOf<RouteGson.Route>()

    var isDistanceOnTrackActive: Boolean = false
        private set

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * [RouteGson.Route] are added to the [Map].
     *
     * @param map       the [Map] associated with the change
     * @param routeList a list of [RouteGson.Route]
     */
    override fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>) {
        Log.d(TAG, routeList.size.toString() + " new route received for map " + map.name)

        drawStaticRoutes()
    }

    override fun onTrackVisibilityChanged() {
        if (::pathView.isInitialized) {
            pathView.invalidate()
        }
    }

    /**
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, mapView: MapView) {
        this.map = map
        setMapView(mapView)
        createPathView()

        if (this.map.areRoutesDefined()) {
            drawStaticRoutes()
            if (isDistanceOnTrackActive) activateDistanceOnTrack()
        } else {
            acquireThenDrawRoutes(this.map)
        }
    }

    fun activateDistanceOnTrack() {
        // TODO: this is just a POC and should be moved elsewhere
        map.routes?.map { route ->
            val view = MarkerGrab(mapView.context)
            view.morphIn()
            val markerMoveAlongRoute = MarkerMoveAlongRoute(route, coroutineScope, mapView, view)
            view.setOnTouchListener(TouchMoveListener(mapView, markerMoveAlongRoute))
            val firstMarker = route.route_markers.firstOrNull()
            if (firstMarker != null) {
                mapView.addMarker(view, firstMarker.proj_x, firstMarker.proj_y, -0.5f, -0.5f)
            }
        }
    }

    fun disableDistanceOnTrack() {

    }

    private fun createPathView() {
        pathView = PathView(mapView.context)
        mapView.addPathView(pathView)
    }

    /**
     * The "live" route will be re-drawn completely, using the same pattern as static routes.
     */
    fun drawLiveRoute(liveRoute: RouteGson.Route) {
        /* Beware, don't make this a class attribute of the flow below will keep a reference on
         * the RouteLayer instance and consequently also the MapView. */
        val liveRoutePaint = Paint().apply {
            this.color = Color.parseColor("#FF9800")
        }
        val liveRouteFlow = drawRoutes(listOf(liveRoute)) { route, path ->
            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = path
                override var paint: Paint? = liveRoutePaint
                override val width: Float? = null
            }
            route.data = drawablePath
            route
        }

        processedLiveRoute.clear()
        coroutineScope.launch {
            liveRouteFlow.collect {
                processedLiveRoute.add(it)
                val allRoutes = processedLiveRoute
                liveRouteView.updatePaths(allRoutes.map { route ->
                    route.data as PathView.DrawablePath
                })
            }
        }
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        getRoutesForMap(map)

        /* Then draw them */
        drawStaticRoutes()
    }

    /**
     * For each route, make a [PathView.DrawablePath] out of the path given by the flow.
     * Then, on the UI thread, render all static routes.
     */
    private fun drawStaticRoutes() {
        val routes = map.routes ?: return
        val staticRouteFlow = drawRoutes(routes) { route, path ->
            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = path
                override var paint: Paint? = null
                override val width: Float? = null
            }
            route.data = drawablePath
            route
        }

        processedStaticRoutes.clear()
        coroutineScope.launch {
            staticRouteFlow.collect {
                processedStaticRoutes.add(it)
                pathView.updatePaths(processedStaticRoutes.map { route ->
                    route.data as PathView.DrawablePath
                })
            }
        }
    }

    /**
     * Returns a [Flow] which asynchronously compute [PathView.DrawablePath] for each route.
     */
    private fun drawRoutes(routeList: List<RouteGson.Route>,
                           action: (RouteGson.Route, FloatArray) -> RouteGson.Route): Flow<RouteGson.Route> {
        return routeList.asFlow().mapNotNull { route ->
            val path = route.toPath(mapView)
            if (path != null) {
                action(route, path)
            } else null
        }.buffer().flowOn(Dispatchers.Default)
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    /**
     * Convert a [RouteGson.Route] to a [FloatArray] which is the drawable data structure expected
     * by the view that will represent it.
     */
    private fun RouteGson.Route.toPath(mapView: MapView): FloatArray? {
        val markerList = route_markers ?: listOf()
        /* If there is only one marker, the path has no sense */
        if (markerList.size < 2) return null

        val size = markerList.size * 4 - 4
        val lines = FloatArray(size)

        var i = 0
        var init = true
        for (marker in markerList) {
            val relativeX = marker.getRelativeX(map)
            val relativeY = marker.getRelativeY(map)
            if (init) {
                lines[i] = mapView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = mapView.coordinateTranslater.translateY(relativeY).toFloat()
                init = false
                i += 2
            } else {
                lines[i] = mapView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = mapView.coordinateTranslater.translateY(relativeY).toFloat()
                if (i + 2 >= size) break
                lines[i + 2] = lines[i]
                lines[i + 3] = lines[i + 1]
                i += 4
            }
        }

        return lines
    }

    private fun MarkerGson.Marker.getRelativeX(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_x else lon
    }

    private fun MarkerGson.Marker.getRelativeY(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_y else lat
    }
}

private class MarkerMoveAlongRoute(route: RouteGson.Route, private val scope: CoroutineScope,
                                   private val mapView: MapView, private val view: View) : TouchMoveListener.MarkerMoveAgent {
    val nearestPointCalculator = NearestMarkerCalculator(route, scope)
    var markerIndexed: MarkerIndexed? = null
        private set

    init {
        scope.launch {
            nearestPointCalculator.nearestMarkersFlow.collect {
                markerIndexed = it
                mapView.moveMarker(view, it.marker.proj_x, it.marker.proj_y)
            }
        }
    }

    override fun onMarkerMove(mapView: MapView?, view: View?, x: Double, y: Double) {
        nearestPointCalculator.updatePosition(x, y)
    }
}

private class RouteRenderer(private val pathView: PathView) {
    private var routes: List<RouteGson.Route> = listOf()
    private var routeWithActiveDistance: RouteGson.Route? = null

    fun setRoutes(routeList: List<RouteGson.Route>) {
        routes = routeList
    }

    fun setRouteWithActiveDistance(route: RouteGson.Route, index1: Int, index2: Int) {
        routeWithActiveDistance = route
    }

    fun render() {
        val drawablePaths = routes.map {
            if (it != routeWithActiveDistance) {
                it.data as PathView.DrawablePath
            } else {
                it.data as PathView.DrawablePath
            }
        }

        pathView.updatePaths(drawablePaths)
    }
}

private const val TAG = "RouteLayer"