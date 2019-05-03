package com.peterlaurence.trekme.ui.mapview

import android.util.Log
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getRoutesForMap
import com.peterlaurence.trekme.ui.mapview.components.PathView
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import com.qozix.tileview.TileView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
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
    private lateinit var mTileView: TileViewExtended
    private lateinit var map: Map

    private var liveRoutePath: MutableList<Float>? = null
    private val TAG = "RouteLayer"

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * [RouteGson.Route] are added to the [Map].
     *
     * @param map       the [Map] associated with the change
     * @param routeList a list of [RouteGson.Route]
     */
    override fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>) {
        Log.d(TAG, routeList.size.toString() + " new route received for map " + map.name)

        drawRoutes()
    }

    override fun onTrackVisibilityChanged() {
        val pathView = mTileView.pathView
        pathView?.invalidate()
    }

    /**
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, tileView: TileView) {
        this.map = map
        setTileView(tileView as TileViewExtended)
        liveRoutePath = null  // cleanup the "live" route path

        if (this.map.areRoutesDefined()) {
            drawRoutes()
        } else {
            acquireThenDrawRoutes(this.map)
        }
    }

    /**
     * If this is the first time the "live" route will be drawn, it's drawn completely using the
     * same pattern as other routes.
     * Right after that, we keep a reference on the path: [liveRoutePath].
     * Subsequent calls will use this path to append new points. Ne need to offload this update into
     * another thread.
     */
    fun updateLiveRoute(route: RouteGson.Route, map: Map) {
        if (this.map == map && liveRoutePath != null) {
            /* Draw partially */
            liveRoutePath?.let { path ->
                val newPath = path.addMarker(
                        route.route_markers.last(), mTileView)
                val existingDrawable = route.data as PathView.DrawablePath
                existingDrawable.path = newPath.toFloatArray()
                mTileView.drawLiveRoute(listOf(route))
            }
        } else {
            /* Redraw completely */
            drawLiveRouteCompletely(route)
        }
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        getRoutesForMap(map)

        /* Then draw them */
        drawRoutes()
    }

    private fun drawRoutes() {
        /* Display all routes */
        map.routes?.let { routes ->
            if (routes.isNotEmpty()) {
                drawRoutes(routes, mTileView) {
                    mTileView.drawRoutes(this)
                }
            }
        }
    }

    private fun drawLiveRouteCompletely(liveRoute: RouteGson.Route) {
        drawRoutes(listOf(liveRoute), mTileView) {
            mTileView.drawLiveRoute(this)
            firstOrNull()?.let {
                liveRoutePath = (it.data as PathView.DrawablePath).path.toMutableList()
            }
        }
    }

    private fun setTileView(tileView: TileViewExtended) {
        mTileView = tileView
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
     */
    private fun CoroutineScope.drawRoutes(routeList: List<RouteGson.Route>,
                                          tileView: TileViewExtended,
                                          action: List<RouteGson.Route>.() -> Unit) = launch {

        val routes = Channel<RouteGson.Route>()
        val paths = Channel<Pair<RouteGson.Route, FloatArray>>()

        repeat(2) {
            producePath(routes, paths, tileView)
        }
        pathProcessor(paths, action)

        routeList.forEach {
            routes.send(it)
        }
    }

    private fun CoroutineScope.pathProcessor(paths: ReceiveChannel<Pair<RouteGson.Route, FloatArray>>,
                                             action: List<RouteGson.Route>.() -> Unit) = launch {
        val routesToDraw = mutableListOf<RouteGson.Route>()
        for ((route, lines) in paths) {
            /* Set the route data */
            val drawablePath = PathView.DrawablePath(lines, null)
            route.apply {
                data = drawablePath
            }
            routesToDraw.add(route)
            routesToDraw.action()
        }
    }

    /**
     * Each [RouteGson.Route] of a [Map] needs to provide data in a format that the
     * [TileView] understands.
     * This is done off UI thread.
     */
    private fun CoroutineScope.producePath(routes: ReceiveChannel<RouteGson.Route>,
                                           paths: SendChannel<Pair<RouteGson.Route, FloatArray>>,
                                           tileView: TileViewExtended) = launch(Dispatchers.Default) {
        for (route in routes) {
            try {
                val lines = route.toPath(tileView) ?: continue
                paths.send(Pair(route, lines))
            } catch (e: Exception) {
                // ignore and continue the loop
            }
        }
    }

    /**
     * Convert a [RouteGson.Route] to a [FloatArray] which is the drawable data structure expected
     * by the view that will represent it.
     */
    private fun RouteGson.Route.toPath(tileView: TileViewExtended): FloatArray? {
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
                lines[i] = tileView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = tileView.coordinateTranslater.translateY(relativeY).toFloat()
                init = false
                i += 2
            } else {
                lines[i] = tileView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = tileView.coordinateTranslater.translateY(relativeY).toFloat()
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

    private fun MutableList<Float>.addMarker(marker: MarkerGson.Marker, tileView: TileViewExtended) = apply {
        val relativeX = marker.getRelativeX(map)
        val relativeY = marker.getRelativeY(map)

        val lines = Array(4) { 0f }

        lines[2] = tileView.coordinateTranslater.translateX(relativeX).toFloat()
        lines[3] = tileView.coordinateTranslater.translateY(relativeY).toFloat()
        if (size >= 2) {
            lines[0] = this[size - 2]
            lines[1] = this[size - 1]
        } else {
            lines[0] = lines[2]
            lines[1] = lines[3]
        }

        addAll(lines)
    }
}
