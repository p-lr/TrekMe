package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.route.Barycenter
import com.peterlaurence.trekme.core.track.toMarker
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePause
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.events.recording.LiveRouteStop
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.ui.mapview.colorLiveRoute
import com.peterlaurence.trekme.ui.mapview.colorRoute
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.Point
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.pow

class RouteLayer(
    scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor,
    private val gpxRecordEvents: GpxRecordEvents
) {
    val isShowingDistanceOnTrack = MutableStateFlow(false)
    private val staticRoutesData = ConcurrentHashMap<Route, RouteData>()

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                staticRoutesData.clear()
                mapInteractor.loadRoutes(map)

                map.routes.collectLatest { routes ->
                    drawStaticRoutes(mapState, map, routes)
                }
            }
        }

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                drawLiveRoute(mapState, map)
            }
        }

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                isShowingDistanceOnTrack.collectLatest {
                    if (it) {
                        val controller = DistanceOnRouteController(mapState, staticRoutesData)
                        controller.processNearestRoute()
                    }
                }
            }
        }
    }

    fun toggleDistanceOnTrack() {
        isShowingDistanceOnTrack.value = !isShowingDistanceOnTrack.value
    }

    private suspend fun drawLiveRoute(mapState: MapState, map: Map): Nothing = coroutineScope {
        val routeList = mutableListOf<Route>()

        fun newRoute(): Route {
            val route = Route(initialColor = colorLiveRoute)
            routeList.add(route)

            launch {
                val pathBuilder = mapState.makePathDataBuilder()
                mapInteractor.getLiveMarkerPositions(map, route).collect {
                    pathBuilder.addPoint(it.x, it.y)
                    val pathData = pathBuilder.build()
                    if (pathData != null) {
                        if (mapState.hasPath(route.id)) {
                            mapState.updatePath(route.id, pathData = pathData)
                        } else {
                            addPath(mapState, route, pathData)
                        }
                    }
                }
            }
            return route
        }

        var route = newRoute()

        gpxRecordEvents.liveRouteFlow.collect {
            when (it) {
                is LiveRoutePoint -> {
                    route.addMarker(it.pt.toMarker(map))
                }
                LiveRouteStop -> {
                    routeList.forEach { route ->
                        mapState.removePath(route.id)
                    }
                    routeList.clear()
                    route = newRoute()
                }
                LiveRoutePause -> {
                    /* Add previous route */
                    routeList.add(route)

                    /* Create and add a new route */
                    route = newRoute()
                }
            }
        }
    }

    /**
     * Anytime this suspend fun is invoked, the parent scope should be cancelled because flows
     * are collected inside [processRoute].
     */
    private suspend fun drawStaticRoutes(mapState: MapState, map: Map, routes: List<Route>) {
        coroutineScope {
            /* First, remove routes which are no longer in the list */
            val routesToRemove = staticRoutesData.keys.filter {
                it !in routes
            }
            routesToRemove.forEach {
                staticRoutesData.remove(it)
                mapState.removePath(it.id)
            }

            /* Then, process current routes */
            for (route in routes) {
                processRoute(route, map, mapState)
            }
        }
    }

    /**
     * Depending on the route's visibility and color, perform required update and/or processing.
     * Notably:
     * * A newly added route have it's [PathData] generated and the corresponding path
     * is added to the [MapState].
     * * A route which was previously added is not re-processed.
     * * When the visibility becomes false, the corresponding path is removed from the [MapState].
     * If the visibility is later set to true, the [PathData] is re-generated.
     */
    private fun CoroutineScope.processRoute(route: Route, map: Map, mapState: MapState) {
        /* React to color change */
        launch(Dispatchers.Default) {
            route.color.collect { color ->
                mapState.updatePath(
                    route.id,
                    color = Color(parseColor(color ?: colorRoute))
                )
            }
        }

        /* React to visibility change */
        launch(Dispatchers.Default) {
            route.visible.collect { visible ->
                val existing = staticRoutesData[route]
                if (visible) {
                    /* Only make route data if it wasn't already processed, or previously removed
                     * after visibility set to false. */
                    val routeData = existing ?: makeRouteData(map, route, mapState)

                    if (routeData != null && !mapState.hasPath(route.id)) {
                        staticRoutesData[route] = routeData
                        addPath(mapState, route, routeData.pathData)
                    }
                } else {
                    if (existing != null) {
                        staticRoutesData.remove(route)
                        mapState.removePath(route.id)
                    }
                }
            }
        }
    }

    private suspend fun makeRouteData(map: Map, route: Route, mapState: MapState): RouteData? {
        val pathBuilder = mapState.makePathDataBuilder()

        var sumX = 0.0
        var sumY = 0.0
        var size = 0
        mapInteractor.getExistingMarkerPositions(map, route).collect {
            pathBuilder.addPoint(it.x, it.y)
            sumX += it.x
            sumY += it.y
            size++
        }

        val pathData = pathBuilder.build()

        val barycenter = if (size > 0) {
            Barycenter(sumX / size, sumY / size)
        } else null

        return pathData?.let {
            RouteData(it, barycenter)
        }
    }

    private fun addPath(mapState: MapState, route: Route, pathData: PathData) {
        mapState.addPath(
            route.id,
            pathData,
            color = route.color.value.let { colorStr ->
                Color(parseColor(colorStr ?: colorRoute))
            }
        )
    }
}

private data class RouteData(val pathData: PathData, val barycenter: Barycenter?)

private class DistanceOnRouteController(
    private val mapState: MapState,
    private val routesData: ConcurrentMap<Route, RouteData>
) {

    suspend fun processNearestRoute() = withContext(Dispatchers.Main) {
        mapState.centroidSnapshotFlow().map {
            findNearestRoute(it)
        }.filterNotNull().distinctUntilChanged().map {
            println("xxxx nearest route : ${it.name}")
        }
    }.collect()

    private suspend fun findNearestRoute(point: Point): Route? = withContext(Dispatchers.Default) {
        routesData.minByOrNull {
            val bary = it.value.barycenter?.let { b -> Point(b.x, b.y) } ?: point
            (point.x - bary.x).pow(2) + (point.y - bary.y).pow(2)
        }?.key
    }
}