package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.route.Barycenter
import com.peterlaurence.trekme.core.track.distanceCalculatorFactory
import com.peterlaurence.trekme.core.track.toMarker
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePause
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.events.recording.LiveRouteStop
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.ui.mapview.colorLiveRoute
import com.peterlaurence.trekme.ui.mapview.colorRoute
import com.peterlaurence.trekme.util.parseColor
import com.peterlaurence.trekme.util.throttle
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.Point
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
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
                val state = DistanceOnRouteController.DistanceOnRouteControllerRestoreState(
                    mutableListOf()
                )
                isShowingDistanceOnTrack.collectLatest {
                    if (it) {
                        val controller = DistanceOnRouteController(
                            map,
                            mapState,
                            mapInteractor,
                            staticRoutesData,
                            state
                        )
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
    private val map: Map,
    private val mapState: MapState,
    private val mapInteractor: MapInteractor,
    private val routesData: ConcurrentMap<Route, RouteData>,
    private val restoreState: DistanceOnRouteControllerRestoreState
) {
    private val grabMarker1 = "distOnRoute-Grab1"
    private val grabMarker2 = "distOnRoute-Grab2"
    private val distMarker = "distOnRoute-Distance"
    private val mainPath = "distOnRoute-MainPath"
    private val headPath = "distOnRoute-HeadPath"
    private val tailPath = "distOnRoute-TailPath"

    suspend fun processNearestRoute() = withContext(Dispatchers.Main) {
        mapState.centroidSnapshotFlow().map {
            findNearestRoute(it)
        }.filterNotNull().distinctUntilChanged().collectLatest { route ->
            coroutineScope {
                launch {
                    drawRoute(route)
                }.invokeOnCompletion {
                    mapState.removeMarker(grabMarker1)
                    mapState.removeMarker(grabMarker2)
                    mapState.removePath(mainPath)
                    mapState.removePath(headPath)
                    mapState.removePath(tailPath)
                    mapState.updatePath(route.id, visible = true)
                }
            }
        }
    }

    private suspend fun drawRoute(route: Route) {
        val routePoints = getRoutePoints(route)
        val chunkSize = min(25, routePoints.size)
        val chunks = routePoints.chunked(chunkSize)
        val chunksByBarycenter = chunks.filter { it.isNotEmpty() }.associateBy {
            getBarycenter(it)
        }
        val distanceComputeFlow = MutableSharedFlow<Unit>(1, 0, BufferOverflow.DROP_OLDEST)
        var distanceText by mutableStateOf("")
        val state = restoreState.states.firstOrNull {
            it.route == route
        } ?: DistanceOnRouteState(route, 0, routePoints.lastIndex / 4).also {
            restoreState.states.add(it)
        }

        val firstPoint = routePoints[state.i1]
        val secondPoint = routePoints[state.i2]

        mapState.addMarker(
            grabMarker1,
            firstPoint.x,
            firstPoint.y,
            relativeOffset = Offset(-0.5f, -0.5f)
        ) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        mapState.addMarker(
            grabMarker2,
            secondPoint.x,
            secondPoint.y,
            relativeOffset = Offset(-0.5f, -0.5f)
        ) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        mapState.addMarker(
            distMarker,
            (firstPoint.x + secondPoint.x) / 2,
            (firstPoint.y + secondPoint.y) / 2,
            relativeOffset = Offset(-0.5f, -0.5f),
            clickable = false,
            clipShape = RoundedCornerShape(5.dp)
        ) {
            Text(
                text = distanceText,
                modifier = Modifier
                    .background(Color(0x885D4037))
                    .padding(horizontal = 4.dp),
                color = Color.White,
                fontSize = 14.sp
            )
        }

        mapState.updatePath(route.id, visible = false)

        val pathData = routesData[route]?.pathData ?: return
        mapState.addPath(
            headPath,
            pathData,
            offset = 0,
            count = min(state.i1, state.i2),
            color = route.color.value.let { colorStr ->
                Color(parseColor(colorStr ?: colorRoute))
            }
        )

        mapState.addPath(
            mainPath,
            pathData,
            width = 10.dp,
            offset = min(state.i1, state.i2),
            count = abs(state.i2 - state.i1),
            color = Color(0xFFF50057)
        )

        mapState.addPath(
            tailPath,
            pathData,
            offset = max(state.i1, state.i2),
            count = routePoints.lastIndex - max(state.i1, state.i2),
            color = route.color.value.let { colorStr ->
                Color(parseColor(colorStr ?: colorRoute))
            }
        )

        fun moveMarker(id: String, px: Double, py: Double): Int? {
            val bary = chunksByBarycenter.keys.minByOrNull {
                (it.x - px).pow(2) + (it.y - py).pow(2)
            } ?: return null

            val chunk = chunksByBarycenter[bary] ?: return null
            return chunk.minByOrNull {
                (it.x - px).pow(2) + (it.y - py).pow(2)
            }?.let {
                mapState.moveMarker(id, it.x, it.y)
                it.index
            }
        }

        fun updatePaths() {
            mapState.updatePath(
                headPath,
                offset = 0,
                count = min(state.i1, state.i2)
            )

            mapState.updatePath(
                mainPath,
                offset = min(state.i1, state.i2),
                count = abs(state.i2 - state.i1)
            )

            mapState.updatePath(
                tailPath,
                offset = max(state.i1, state.i2),
                count = routePoints.lastIndex - max(state.i1, state.i2)
            )
        }

        fun updateDistance() {
            val p1 = routePoints[state.i1]
            val p2 = routePoints[state.i2]
            mapState.moveMarker(
                distMarker,
                x = (p1.x + p2.x) / 2,
                y = (p1.y + p2.y) / 2,
            )
            distanceComputeFlow.tryEmit(Unit)
        }

        mapState.enableMarkerDrag(grabMarker1) { _, _, _, _, _, px, py ->
            val index = moveMarker(grabMarker1, px, py)
            if (index != null) {
                state.i1 = index
                updatePaths()
                updateDistance()
            }
        }

        mapState.enableMarkerDrag(grabMarker2) { _, _, _, _, _, px, py ->
            val index = moveMarker(grabMarker2, px, py)
            if (index != null) {
                state.i2 = index
                updatePaths()
                updateDistance()
            }
        }

        /* Trigger the first distance computation (don't wait grab markers to be moved) */
        distanceComputeFlow.tryEmit(Unit)

        /* This collection never completes - this is on purpose */
        distanceComputeFlow.throttle(100).collect {
            val dist = computeDistance(route, state.i1, state.i2)
            distanceText = UnitFormatter.formatDistance(dist)
        }
    }

    private suspend fun findNearestRoute(point: Point): Route? = withContext(Dispatchers.Default) {
        routesData.minByOrNull {
            val bary = it.value.barycenter?.let { b -> Point(b.x, b.y) } ?: point
            (point.x - bary.x).pow(2) + (point.y - bary.y).pow(2)
        }?.key
    }

    private suspend fun getRoutePoints(route: Route): List<PointIndexed> {
        var i = 0
        return mapInteractor.getExistingMarkerPositions(map, route).map {
            PointIndexed(i++, it.x, it.y)
        }.toList()
    }

    private fun getBarycenter(chunk: List<PointIndexed>): Barycenter {
        var sumX = 0.0
        var sumY = 0.0
        for (pt in chunk) {
            sumX += pt.x
            sumY += pt.y
        }
        return Barycenter(sumX / chunk.size, sumY / chunk.size)
    }

    private suspend fun computeDistance(route: Route, i1: Int, i2: Int): Double {
        return withContext(Dispatchers.Default) {
            val iMin = min(i1, i2)
            val iMax = max(i1, i2)

            val iterator = route.routeMarkers.listIterator(iMin)

            val distanceCalculator = distanceCalculatorFactory(route.elevationTrusted)
            for (i in iMin until iMax) {
                val marker = iterator.next()
                distanceCalculator.addPoint(marker.lat, marker.lon, marker.elevation)
            }

            distanceCalculator.getDistance()
        }
    }

    private data class PointIndexed(val index: Int, val x: Double, val y: Double)

    data class DistanceOnRouteControllerRestoreState(val states: MutableList<DistanceOnRouteState>)
    data class DistanceOnRouteState(val route: Route, var i1: Int, var i2: Int)
}