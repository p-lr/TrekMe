package com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.wmts.domain.model.Point
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.visibleArea
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID

class TrackCreateLayer(
    private val scope: CoroutineScope,
    private val mapState: MapState
) {
    val trackState = MutableStateFlow<List<TrackSegmentState>>(emptyList())
    val hasUndoState = MutableStateFlow(false)
    val hasRedoState = MutableStateFlow(false)
    private val undoStack = ArrayDeque<Action>()
    private val redoStack = ArrayDeque<Action>()

    fun undo() {
        val action = popUndo() ?: return

        when(action) {
            is Action.AddPoint -> removePoint(action.pointState.id)
            is Action.MovePoint -> movePoint(action.pointState, action.from)
            is Action.SplitSegment -> {
                val prev = action.newPoint.prev
                val next = action.newPoint.next
                if (prev != null && next != null) {
                    fuseSegments(prev, next, action.previousSegment)
                }
            }
        }

        addActionToRedoStack(action)
    }

    fun reDo() {
        val action = popRedo() ?: return

        when(action) {
            is Action.AddPoint -> {
                val lastPointState = trackState.value.lastOrNull()?.p2 ?: action.pointState.prev?.p1
                val segment = action.pointState.prev
                if (lastPointState != null && segment != null) {
                    lastPointState.next = segment
                    addPoint(action.pointState, segment)
                }
            }
            is Action.MovePoint -> movePoint(action.pointState, action.to)
            is Action.SplitSegment -> {
                val prev = action.newPoint.prev
                val next = action.newPoint.next
                if (prev != null && next != null) {
                    undoFuseSegments(prev, next, action.previousSegment)
                }
            }
        }

        addActionToUndoStack(action)
    }

    init {
        initialize()
    }

    private fun initialize() = scope.launch {
        val area = mapState.visibleArea()
        val p1x = (0.5 * area.p1x + 0.5 * area.p3x).coerceIn(0.0..1.0)
        val p1y = (0.5 * area.p1y + 0.5 * area.p3y).coerceIn(0.0..1.0)

        val firstPointId = makeMarkerId()

        mapState.addMarker(firstPointId, p1x, p1y, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        val p1State = PointState(firstPointId, p1x, p1y, markerId = firstPointId)
        configureDrag(p1State)

        mapState.onTap { x, y ->
            val lastSegmentState = trackState.value.lastOrNull()
            val lastPointState = lastSegmentState?.p2 ?: p1State
            val newMarkerId = makeMarkerId()
            val newPointState = PointState(newMarkerId, x, y, markerId = newMarkerId)
            val newSegment = TrackSegmentState(
                id = makeSegmentId(),
                p1 = lastPointState,
                p2 = newPointState
            )
            newPointState.prev = newSegment
            lastPointState.next = newSegment

            addPoint(newPointState, newSegment)

            /* This is a user gesture: add an action to undo stack and clear redo stack */
            addActionToUndoStack(Action.AddPoint(newPointState))
            clearRedoStack()
        }
    }

    private fun addPoint(newPointState: PointState, segmentState: TrackSegmentState) {
        trackState.update {
            it + segmentState
        }

        configureCenterDrag(segmentState)

        mapState.addMarker(newPointState.id, newPointState.x, newPointState.y, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }
        configureDrag(newPointState)
    }

    private fun removePoint(id: String) {
        trackState.update {
            val indexesFound = mutableListOf<Int>()
            for ((i, s) in it.withIndex()) {
                if (s.p1.id == id || s.p2.id == id) {
                    indexesFound.add(i)
                }
            }
            when (indexesFound.size) {
                0 -> it
                1 -> {  // head or tail remove
                    val index = indexesFound.first()
                    val segment = it[index]
                    if (segment.p1.id == id) { // removing the very first point
                        println("xxxxx removing first point")
                        it
                    } else {  // removing the last point
                        println("xxxxx removing last point")
                        segment.p1.next = null
                        mapState.removeMarker(id)
                        mapState.removeMarker(segment.centerId)
                        it - it.last()
                    }
                }
                else -> { // indexesFound.size must be 2, in this case removing a point in between
                    println("xxxxx removing in between")
                    it
                }
            }
        }
    }

    private fun fuseSegments(
        previousToRemove: TrackSegmentState,
        nextToRemove: TrackSegmentState,
        oldToRestore: TrackSegmentState
    ) {
        trackState.update {
            var hasReplaced = false
            buildList {
                for (s in it) {
                    if (s.id == nextToRemove.id) {
                        continue
                    }
                    if (s.id == previousToRemove.id && !hasReplaced) {
                        add(oldToRestore)
                        hasReplaced = true
                    } else {
                        add(s)
                    }
                }
            }
        }
        oldToRestore.p1.next = oldToRestore
        oldToRestore.p2.prev = oldToRestore

        mapState.removeMarker(previousToRemove.centerId)
        mapState.removeMarker(nextToRemove.centerId)
        mapState.removeMarker(oldToRestore.centerId)
        configureCenterDrag(oldToRestore)
    }

    private fun undoFuseSegments(
        previousToRemove: TrackSegmentState,
        nextToRemove: TrackSegmentState,
        oldToRestore: TrackSegmentState
    ) {
        trackState.update {
            var hasReplaced = false
            buildList {
                for (s in it) {
                    if (s.id == oldToRestore.id && !hasReplaced) {
                        add(previousToRemove)
                        add(nextToRemove)
                        hasReplaced = true
                    } else {
                        add(s)
                    }
                }
            }
        }

        previousToRemove.p1.next = previousToRemove
        nextToRemove.p2.prev = nextToRemove

        configureCenterDrag(previousToRemove)
        configureCenterDrag(nextToRemove)
        mapState.moveMarker(
            previousToRemove.p2.markerId, previousToRemove.p2.x, previousToRemove.p2.y
        )
        configureDrag(previousToRemove.p2)
    }

    private fun movePoint(pointState: PointState, to: Point) {
        pointState.x = to.X
        pointState.y = to.Y

        moveMarkers(pointState, to.X, to.Y)
    }

    private fun configureCenterDrag(
        segmentState: TrackSegmentState,
    ) {
        val centerId = segmentState.centerId
        val centerX = (segmentState.p1.x + segmentState.p2.x) / 2
        val centerY = (segmentState.p1.y + segmentState.p2.y) / 2
        mapState.addMarker(centerId, centerX, centerY, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 25.dp)
        }
        var split: PointState? = null
        var dragStart: Point? = null
        mapState.enableMarkerDrag(
            centerId,
            onDragStart = { _, x, y ->
                if (split == null) {
                    val newPoint = splitSegment(segmentState)
                    split = newPoint

                    /* This is a user gesture: add an action to undo stack and clear redo stack */
                    addActionToUndoStack(Action.SplitSegment(segmentState, newPoint))
                    clearRedoStack()
                } else {
                    /* Only remember the drag start after a first split is done */
                    dragStart = Point(x, y)
                }
            },
            onDragEnd = { _, x, y ->
                val from = dragStart
                val splitPoint = split
                if (from != null && splitPoint != null) {
                    /* This is a user gesture: add an action to undo stack and clear redo stack */
                    addActionToUndoStack(Action.MovePoint(splitPoint, from = from, to = Point(x, y)))
                    clearRedoStack()
                }
            }
        ) { _, x, y, dx, dy, _, _ ->
            split?.also { pt ->
                pt.x = x + dx
                pt.y = y + dy
                moveMarkers(pt, x + dx, y + dy)
            }
        }
    }

    /**
     * Since this method creates new segments, it must be invoked on user action only (it must not
     * be directly invoked when undoing or redoing).
     */
    private fun splitSegment(segmentState: TrackSegmentState): PointState {
        val splitPoint = PointState(
            id = makeMarkerId(),
            x = (segmentState.p1.x + segmentState.p2.x) / 2,
            y = (segmentState.p1.y + segmentState.p2.y) / 2,
            markerId = segmentState.centerId
        )

        val segment1 = TrackSegmentState(
            id = makeSegmentId(),
            p1 = segmentState.p1,
            p2 = splitPoint
        )

        val segment2 = TrackSegmentState(
            id = makeSegmentId(),
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

        configureCenterDrag(segment1)
        configureCenterDrag(segment2)

        return splitPoint
    }

    private fun configureDrag(pointState: PointState) {
        var dragStart: Point? = null
        mapState.enableMarkerDrag(
            pointState.markerId,
            onDragStart = { _, x, y ->
                dragStart = Point(x, y)
            },
            onDragEnd = { _, x, y ->
                val from = dragStart
                if (from != null) {
                    /* This is a user gesture: add an action to undo stack and clear redo stack */
                    addActionToUndoStack(Action.MovePoint(pointState, from, to = Point(x, y)))
                    clearRedoStack()
                }
            }
        ) { _, x, y, dx, dy, _, _ ->
            pointState.x = x + dx
            pointState.y = y + dy
            moveMarkers(pointState, x + dx, y + dy)
        }
    }

    private fun moveMarkers(pointState: PointState, x: Double, y: Double) {
        mapState.moveMarker(pointState.markerId, x, y)
        pointState.prev?.also {
            mapState.moveMarker(it.centerId, (it.p1.x + x) / 2, (it.p1.y + y) / 2)
        }
        pointState.next?.also {
            mapState.moveMarker(it.centerId, (it.p2.x + x) / 2, (it.p2.y + y) / 2)
        }
    }

    private fun popUndo(): Action? {
        return undoStack.removeLastOrNull().also {
            hasUndoState.value = undoStack.size > 0
        }
    }

    private fun popRedo(): Action? {
        return redoStack.removeLastOrNull().also {
            hasRedoState.value = redoStack.size > 0
        }
    }

    private fun addActionToUndoStack(action: Action) {
        undoStack.add(action)
        hasUndoState.value = true

        println("xxxxx undo: ${undoStack.joinToString(",") { it.toString() }}")
    }

    private fun addActionToRedoStack(action: Action) {
        redoStack.add(action)
        hasRedoState.value = true
    }

    private fun clearRedoStack() {
        redoStack.clear()
        hasRedoState.value = false
    }
}

data class TrackSegmentState(val id: String, val p1: PointState, val p2: PointState) {
    val centerId = makeMarkerId()
}

private fun makeMarkerId(): String = UUID.randomUUID().toString()
private fun makeSegmentId(): String = UUID.randomUUID().toString()

/**
 * Each [PointState] has its own handle (marker). Sometimes, the marker id corresponds to the
 * PointState [id] (e.g when adding a new point at the tail of the track). However, when splitting
 * a segment by dragging the middle point, the marker id equals the segment's centerId.
 */
class PointState(val id: String, x: Double, y: Double, val markerId: String) {
    var x: Double by mutableDoubleStateOf(x)
    var y: Double by mutableDoubleStateOf(y)
    var next: TrackSegmentState? = null
    var prev: TrackSegmentState? = null

    override fun toString(): String {
        return """PointState(x=$x, y=$y, next=${next?.id}, prev=${prev?.id})"""
    }
}

sealed interface Action {
    data class AddPoint(val pointState: PointState): Action
    data class MovePoint(val pointState: PointState, val from: Point, val to: Point): Action
    data class SplitSegment(val previousSegment: TrackSegmentState, val newPoint: PointState): Action
}
