package com.peterlaurence.trekme.ui.mapview

import android.graphics.Paint
import android.util.Log
import android.view.View
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.mapview.paths.PathView
import com.peterlaurence.mapview.paths.addPathView
import com.peterlaurence.trekme.R
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
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
    private lateinit var liveRouteView: PathView

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
        createLiveRouteView()


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

    private fun createLiveRouteView() {
        val context = mapView.context
        liveRouteView = PathView(context)
        liveRouteView.color = context.getColor(R.color.colorLiveRoute)
        mapView.addPathView(liveRouteView)
    }

    /**
     * The "live" route will be re-drawn completely, using the same pattern as other routes.
     */
    fun drawLiveRoute(liveRoute: RouteGson.Route) {
        computePaths(listOf(liveRoute), mapView) {
            liveRouteView.updatePaths(this.map { it.data as PathView.DrawablePath })
        }
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        getRoutesForMap(map)

        /* Then draw them */
        drawStaticRoutes()
    }

    private fun drawStaticRoutes() {
        val routes = map.routes ?: return
        drawRoutes(routes)
    }

    /**
     * Use [Flow] to asynchronously compute [PathView.DrawablePath] for each route.
     * Then, on the UI thread, trigger the render.
     */
    private fun drawRoutes(routeList: List<RouteGson.Route>) {
        val routeFlow = routeList.asFlow().mapNotNull { route ->
            val path = route.toPath(mapView)
            if (path != null) {
                val drawablePath = object : PathView.DrawablePath {
                    override val visible: Boolean
                        get() = route.visible
                    override var path: FloatArray = path
                    override var paint: Paint? = null
                    override val width: Float? = null
                }
                route.data = drawablePath
                route
            } else null
        }.buffer().flowOn(Dispatchers.Default)

        val processedRoutes = mutableListOf<RouteGson.Route>()
        coroutineScope.launch {
            routeFlow.collect {
                processedRoutes.add(it)
                pathView.updatePaths(processedRoutes.map { route ->
                    route.data as PathView.DrawablePath
                })
            }
        }
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    /**
     * Use [Channel]s to communicate between coroutines and ensure no concurrent access on resources.
     * A [Channel] is represented here with ****
     *
     *                      Worker pool
     *                   _________________
     *   routes         |  _____________  |         paths          _______________
     * [********] ----> | | producePath | | ----> [*******] ----> | pathProcessor |
     *                  |  -------------  |                        ---------------
     *                  |  _____________  |
     *                  | | producePath | |
     *                  |  -------------  |
     *                  ------------------
     * TODO: this could probably be implemented with Flows, as this is one-shot processing machinery
     */
    private fun CoroutineScope.computePaths(routeList: List<RouteGson.Route>,
                                            mapView: MapView,
                                            action: List<RouteGson.Route>.() -> Unit) = launch {

        val routes = Channel<RouteGson.Route>()
        val paths = Channel<Pair<RouteGson.Route, FloatArray>>()

        repeat(2) {
            producePath(routes, paths, mapView)
        }
        pathProcessor(paths, action)

        routeList.forEach {
            routes.send(it)
        }
    }

    /**
     * Each [RouteGson.Route] of a [Map] needs to provide data in a format that the
     * [MapView] understands (e.g [PathView] accepts path data as [FloatArray]).
     * This is done off UI thread.
     */
    private fun CoroutineScope.producePath(routes: ReceiveChannel<RouteGson.Route>,
                                           paths: SendChannel<Pair<RouteGson.Route, FloatArray>>,
                                           mapView: MapView) = launch(Dispatchers.Default) {
        for (route in routes) {
            try {
                val lines = route.toPath(mapView) ?: continue
                paths.send(Pair(route, lines))
            } catch (e: Exception) {
                // ignore and continue the loop
            }
        }
    }

    private fun CoroutineScope.pathProcessor(paths: ReceiveChannel<Pair<RouteGson.Route, FloatArray>>,
                                             action: List<RouteGson.Route>.() -> Unit) = launch {
        val routesToDraw = mutableListOf<RouteGson.Route>()
        for ((route, lines) in paths) {
            /* Set the route data */
            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = lines
                override var paint: Paint? = null
                override val width: Float? = null
            }
            route.apply {
                data = drawablePath
            }
            routesToDraw.add(route)
            routesToDraw.action()
        }
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