package com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers

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
import com.peterlaurence.trekme.core.georecord.domain.logic.distanceCalculatorFactory
import com.peterlaurence.trekme.core.map.domain.models.Barycenter
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.presentation.model.RouteData
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.util.parseColor
import com.peterlaurence.trekme.util.throttle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.Point
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DistanceOnRouteController(
    private val map: Map,
    private val mapState: MapState,
    private val routeInteractor: RouteInteractor,
    private val routesDataFlow: Flow<kotlin.collections.Map<Route, RouteData>>,
    private val restoreState: DistanceOnRouteControllerRestoreState
) {
    private val prefix = "distOnRoute"
    private val grabMarker1 = "$prefix-Grab1"
    private val grabMarker2 = "$prefix-Grab2"
    private val distMarker = "$prefix-Distance"
    private val mainPath = "$prefix-MainPath"
    private val headPath = "$prefix-HeadPath"
    private val tailPath = "$prefix-TailPath"

    suspend fun processNearestRoute() = withContext(Dispatchers.Main) {
        combine(mapState.centroidSnapshotFlow(), routesDataFlow) { centroid, routesData ->
            val route = findNearestRoute(centroid, routesData)
            route?.let { it to routesData }
        }.filterNotNull().distinctUntilChanged().collectLatest { (route, routesData) ->
            coroutineScope {
                launch {
                    drawSegments(route, routesData)
                }.invokeOnCompletion {
                    mapState.removeMarker(grabMarker1)
                    mapState.removeMarker(grabMarker2)
                    mapState.removeMarker(distMarker)
                    mapState.removePath(mainPath)
                    mapState.removePath(headPath)
                    mapState.removePath(tailPath)
                    mapState.updatePath(route.id, visible = true)
                }
            }
        }
    }

    /**
     * Draws the red segment between the two handles, and the other two remaining segments.
     */
    private suspend fun drawSegments(route: Route, routesData: kotlin.collections.Map<Route, RouteData>) {
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
                Color(parseColor(colorStr))
            }
        )

        mapState.addPath(
            mainPath,
            pathData,
            width = 5.dp,
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
                Color(parseColor(colorStr))
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

    private suspend fun findNearestRoute(
        point: Point,
        routesData: kotlin.collections.Map<Route, RouteData>
    ): Route? = withContext(Dispatchers.Default) {
        routesData.minByOrNull {
            val bary = it.value.barycenter.let { b -> Point(b.x, b.y) }
            (point.x - bary.x).pow(2) + (point.y - bary.y).pow(2)
        }?.key
    }

    private suspend fun getRoutePoints(route: Route): List<PointIndexed> {
        var i = 0
        return routeInteractor.getExistingMarkerPositions(map, route).map {
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