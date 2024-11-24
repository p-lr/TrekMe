package com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.visibleArea
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID

class TrackCreateLayer(
    private val scope: CoroutineScope
) {
    val trackState = MutableStateFlow<List<TrackSegmentState>>(emptyList())

    fun init(mapState: MapState) = scope.launch {
        val area = mapState.visibleArea()
        val p1x = (0.5 * area.p1x + 0.5 * area.p3x).coerceIn(0.0..1.0)
        val p1y = (0.5 * area.p1y + 0.5 * area.p3y).coerceIn(0.0..1.0)

        val firstPointId = makeMarkerId()

        mapState.addMarker(firstPointId, p1x, p1y, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        val p1State = PointState(firstPointId, p1x, p1y)
        configureDrag(mapState, firstPointId, p1State)

        mapState.onTap { x, y ->
            val lastSegmentState = trackState.value.lastOrNull()
            val lastPointState = lastSegmentState?.p2 ?: p1State
            val newMarkerId = makeMarkerId()
            val newPointState = PointState(newMarkerId, x, y)
            val newSegment = TrackSegmentState(
                id = lastPointState.id,
                p1 = lastPointState,
                p2 = newPointState
            )
            newPointState.prev = newSegment
            lastPointState.next = newSegment
            trackState.update {
                it + newSegment
            }

            configureCenterDrag(
                mapState,
                newSegment,
                centerX = (x + lastPointState.x) / 2.0,
                centerY = (y + lastPointState.y) / 2.0
            )

            mapState.addMarker(newMarkerId, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                MarkerGrab(morphedIn = true, size = 50.dp)
            }
            configureDrag(mapState, newMarkerId, newPointState)
        }
    }

    private fun configureCenterDrag(
        mapState: MapState,
        segmentState: TrackSegmentState,
        centerX: Double,
        centerY: Double
    ) {
        val centerId = segmentState.centerId
        mapState.addMarker(centerId, centerX, centerY, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 25.dp)
        }
        var split: PointState? = null
        mapState.enableMarkerDrag(centerId) { _, x, y, dx, dy, _, _ ->
            split?.also { pt ->
                pt.x = x + dx
                pt.y = y + dy
                moveMarkers(mapState, centerId, pt, x + dx, y + dy)
            } ?: run {
                split = splitSegment(segmentState, mapState)
            }
        }
    }

    private fun splitSegment(segmentState: TrackSegmentState, mapState: MapState): PointState {
        val splitPoint = PointState(
            id = makeMarkerId(),
            x = (segmentState.p1.x + segmentState.p2.x) / 2,
            y = (segmentState.p1.y + segmentState.p2.y) / 2
        )

        val segment1 = TrackSegmentState(
            id = UUID.randomUUID().toString(),
            p1 = segmentState.p1,
            p2 = splitPoint
        )

        val segment2 = TrackSegmentState(
            id = UUID.randomUUID().toString(),
            p1 = splitPoint,
            p2 = segmentState.p2
        )
        splitPoint.prev = segment1
        splitPoint.next = segment2
        segmentState.p1.next = segment1
        segmentState.p2.prev = segment2

        trackState.update {
            buildList {
                for (s in it) {
                    if (s.id == segmentState.id) {
                        add(segment1)
                        add(segment2)
                    } else {
                        add(s)
                    }
                }
            }
        }

        configureCenterDrag(
            mapState,
            segment1,
            centerX = (segmentState.p1.x + splitPoint.x) / 2.0,
            centerY = (segmentState.p1.y + splitPoint.y) / 2.0
        )
        configureCenterDrag(
            mapState,
            segment2,
            centerX = (splitPoint.x + segmentState.p2.x) / 2.0,
            centerY = (splitPoint.y + segmentState.p2.y) / 2.0
        )

        return splitPoint
    }

    private fun makeMarkerId(): String {
        return UUID.randomUUID().toString()
    }

    private fun configureDrag(mapState: MapState, markerId: String, pointState: PointState) {
        mapState.enableMarkerDrag(markerId) { _, x, y, dx, dy, _, _ ->
            pointState.x = x + dx
            pointState.y = y + dy
            moveMarkers(mapState, markerId, pointState, x + dx, y + dy)
        }
    }

    private fun moveMarkers(mapState: MapState, markerId: String, pointState: PointState, x: Double, y: Double) {
        mapState.moveMarker(markerId, x, y)
        pointState.prev?.also {
            mapState.moveMarker(it.centerId, (it.p1.x + x) / 2, (it.p1.y + y) / 2)
        }
        pointState.next?.also {
            mapState.moveMarker(it.centerId, (it.p2.x + x) / 2, (it.p2.y + y) / 2)
        }
    }
}

data class TrackSegmentState(val id: String, val p1: PointState, val p2: PointState) {
    val centerId = UUID.randomUUID().toString()
}

class PointState(val id: String, x: Double, y: Double) {
    var x: Double by mutableDoubleStateOf(x)
    var y: Double by mutableDoubleStateOf(y)
    var next: TrackSegmentState? = null
    var prev: TrackSegmentState? = null
}
