package com.peterlaurence.trekme.ui.mapview

import android.util.Log
import com.peterlaurence.trekme.core.map.Map
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
 * All [RouteGson.Route] are managed here. <br></br>
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
    private lateinit var mMap: Map
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
        mMap = map
        setTileView(tileView as TileViewExtended)

        if (mMap.areRoutesDefined()) {
            drawRoutes()
        } else {
            acquireThenDrawRoutes(mMap)
        }
    }

    fun updateLiveRoute(route: RouteGson.Route, map: Map) {
        if (mMap == map) {
            println("Update live route size ${route.route_markers.size}")
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
        mMap.routes?.let { routes ->
            if (routes.isNotEmpty()) {
                drawRoutes(routes, mTileView)
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
     *   routes          _____________          paths          _______________
     * [********] ----> | producePath | ----> [*******] ----> | pathProcessor |
     *                   -------------                         ---------------
     */
    private fun CoroutineScope.drawRoutes(routeList: List<RouteGson.Route>, tileView: TileViewExtended) = launch {

        val routes = Channel<RouteGson.Route>()
        val paths = Channel<Pair<RouteGson.Route, FloatArray>>()

        producePath(routes, paths, tileView)
        pathProcessor(paths, tileView)

        routeList.forEach {
            routes.send(it)
        }
    }

    private fun CoroutineScope.pathProcessor(paths: ReceiveChannel<Pair<RouteGson.Route, FloatArray>>,
                                             tileView: TileViewExtended) = launch {
        val routesToDraw = mutableListOf<RouteGson.Route>()
        for ((route, lines) in paths) {
            /* Set the route data */
            val drawablePath = PathView.DrawablePath(lines, null)
            route.apply {
                data = drawablePath
            }
            routesToDraw.add(route)
            tileView.drawRoutes(routesToDraw)
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
                val markerList = route.route_markers ?: listOf()
                /* If there is only one marker, the path has no sense */
                if (markerList.size < 2) continue

                val size = markerList.size * 4 - 4
                val lines = FloatArray(size)

                var i = 0
                var init = true
                val mapUsesProjection = mMap.projection != null
                for (marker in markerList) {
                    val relativeX = if (mapUsesProjection) marker.proj_x else marker.lon
                    val relativeY = if (mapUsesProjection) marker.proj_y else marker.lat
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

                paths.send(Pair(route, lines))
            } catch (e: Exception) {
                // ignore and continue the loop
            }
        }
    }
}
