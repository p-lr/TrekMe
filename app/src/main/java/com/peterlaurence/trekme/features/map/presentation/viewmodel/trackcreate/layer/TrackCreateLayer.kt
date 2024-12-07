@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalClusteringApi::class)

package com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.Point
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.Custom
import ovh.plrapps.mapcompose.api.ExperimentalClusteringApi
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.visibleArea
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy
import java.util.UUID

class TrackCreateLayer(
    private val scope: CoroutineScope,
    private val mapState: MapState
) {
    val trackState = MutableStateFlow<List<TrackSegmentState>>(emptyList())
    val hasUndoState = MutableStateFlow(false)
    val hasRedoState = MutableStateFlow(false)
    val lastUndoActionId = MutableStateFlow<String?>(null)
    private val undoStack = ArrayDeque<Action>()
    private val redoStack = ArrayDeque<Action>()

    fun undo() {
        val action = popUndo() ?: return

        when (action) {
            is Action.AddPoint -> removeLastPoint(action.pointState)
            is Action.MovePoint -> movePoint(action.pointState, action.from)
            is Action.SplitSegment -> {
                val prev = action.newPoint.prev
                val next = action.newPoint.next
                if (prev != null && next != null) {
                    fuseSegments(prev, next, action.previousSegment)
                }
            }

            is Action.RemoveMiddlePoint -> undoRemoveMiddlePoint(action)
            is Action.RemoveLastPoint -> redoAddLastPoint(action.pointState)
        }

        addActionToRedoStack(action)
        mapState.removeCallout(calloutId)
    }

    fun reDo() {
        val action = popRedo() ?: return

        when (action) {
            is Action.AddPoint -> redoAddLastPoint(action.pointState)
            is Action.MovePoint -> movePoint(action.pointState, action.to)
            is Action.SplitSegment -> {
                val prev = action.newPoint.prev
                val next = action.newPoint.next
                if (prev != null && next != null) {
                    undoFuseSegments(prev, next, action.previousSegment)
                }
            }

            is Action.RemoveMiddlePoint -> redoRemoveMiddlePoint(action)
            is Action.RemoveLastPoint -> removeLastPoint(action.pointState)
        }

        addActionToUndoStack(action)
        mapState.removeCallout(calloutId)
    }

    init {
        configureClustering()
    }

    private fun configureClustering() {
        mapState.addClusterer(clustererId,
            clusteringThreshold = 25.dp,
            clusterClickBehavior = Custom(
                withDefaultBehavior = false,
                onClick = { _ -> })
        ) { _ ->
            {  }  // When markers are too close to each other, don't display anything
        }
    }

    /* region restore from existing */
    fun addFirstSegment(x1: Double, y1: Double, x2: Double, y2: Double): PointState {
        val p1State = addStartPoint(x1, y1)
        addSegment(lastPointState = p1State, x2, y2)
        return p1State
    }

    fun addExistingPoint(x: Double, y: Double) {
        val lastSegmentState = trackState.value.lastOrNull() ?: return

        val lastPointState = lastSegmentState.p2
        addSegment(lastPointState, x, y)
    }
    /* endregion */

    fun initialize(firstPointState: PointState) = scope.launch {
        mapState.onTap { x, y ->
            val lastSegmentState = trackState.value.lastOrNull()
            val lastPointState = lastSegmentState?.p2 ?: firstPointState
            val newPointState = addSegment(lastPointState, x, y)

            /* This is a user gesture: add an action to undo stack and clear redo stack */
            addActionToUndoStack(Action.AddPoint(newPointState))
            clearRedoStack()
        }

        mapState.onMarkerClick { id, x, y ->
            /* Click on first marker is no-op. We do that instead of making the first marker
             * non-clickable to avoid accidentally creating a new point when clicking on first
             * marker. */
            if (id == firstPointState.id) return@onMarkerClick
            if (isCenterOfSegment(id)) return@onMarkerClick

            mapState.addCallout(calloutId, x, y) {
                SmallFloatingActionButton(
                    onClick = {
                        val pointState = getPointState(id) ?: return@SmallFloatingActionButton
                        val prev = pointState.prev ?: return@SmallFloatingActionButton
                        val next = pointState.next

                        if (next != null) {
                            val newSegment = removeMiddlePoint(pointState, next.p2)

                            if (newSegment != null) {
                                /* This is a user gesture: add an action to undo stack and clear redo stack */
                                addActionToUndoStack(
                                    Action.RemoveMiddlePoint(
                                        prev,
                                        next,
                                        newSegment
                                    )
                                )
                                clearRedoStack()
                            }
                        } else {
                            removeLastPoint(pointState)

                            /* This is a user gesture: add an action to undo stack and clear redo stack */
                            addActionToUndoStack(
                                Action.RemoveLastPoint(pointState)
                            )
                            clearRedoStack()
                        }

                        mapState.removeCallout(calloutId)
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_forever_black_24dp),
                        contentDescription = null
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun initializeNewTrack() = scope.launch {
        val area = mapState.visibleArea()
        val p1x = (0.5 * area.p1x + 0.5 * area.p3x).coerceIn(0.0..1.0)
        val p1y = (0.5 * area.p1y + 0.5 * area.p3y).coerceIn(0.0..1.0)

        val p1State = addStartPoint(p1x, p1y)

        initialize(p1State)

        /* Show tooltip on first point */
        val tooltipState = TooltipState(isPersistent = false)
        showStartToolTip(tooltipState, p1x, p1y)
    }

    private fun addStartPoint(x: Double, y: Double): PointState {
        val id = makeMarkerId()
        addMarkerGrab(id, x, y, color = { startMarkerColor })
        val p1State = PointState(id, x, y, markerId = id)
        configureDrag(p1State)
        return p1State
    }

    private fun addSegment(lastPointState: PointState, x: Double, y: Double): PointState {
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
        return newPointState
    }

    private fun redoAddLastPoint(pointState: PointState) {
        val lastPointState = trackState.value.lastOrNull()?.p2 ?: pointState.prev?.p1
        val segment = pointState.prev
        if (lastPointState != null && segment != null) {
            lastPointState.next = segment
            addPoint(pointState, segment)
        }
    }

    private fun addPoint(newPointState: PointState, segmentState: TrackSegmentState) {
        trackState.update {
            it + segmentState
        }

        configureCenterDrag(segmentState)

        addMarkerGrab(
            id = newPointState.markerId,
            x = newPointState.x,
            y = newPointState.y,
            color = { pointMarkerColor }
        )
        configureDrag(newPointState)
    }

    private fun removeMiddlePoint(
        pointState: PointState,
        nextPoint: PointState
    ): TrackSegmentState? {
        var newSegment: TrackSegmentState? = null
        trackState.update {
            buildList {
                for (s in it) {
                    when {
                        s.p2.id == pointState.id -> {
                            mapState.removeMarker(pointState.markerId)
                            mapState.removeMarker(s.centerId)

                            val segment = TrackSegmentState(
                                id = makeSegmentId(),
                                p1 = s.p1,
                                p2 = nextPoint
                            )
                            newSegment = segment

                            s.p1.next = newSegment
                            nextPoint.prev = newSegment
                            configureCenterDrag(segment)

                            add(segment)
                        }

                        s.p1.id == pointState.id -> {
                            mapState.removeMarker(s.centerId)
                        }

                        else -> add(s)
                    }
                }
            }
        }
        return newSegment
    }

    private fun removeLastPoint(pointState: PointState) {
        val centerId = pointState.prev?.centerId ?: return
        mapState.removeMarker(pointState.markerId)
        mapState.removeMarker(centerId)
        pointState.prev?.p1?.next = null

        trackState.update {
            if (it.isNotEmpty()) {
                it - it.last()
            } else it
        }
    }

    private fun redoRemoveMiddlePoint(action: Action.RemoveMiddlePoint) {
        mapState.removeMarker(action.previousSegment.p2.markerId)
        fuseSegments(
            previousToRemove = action.previousSegment,
            nextToRemove = action.nextSegment,
            oldToRestore = action.newSegment
        )
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

    private fun undoRemoveMiddlePoint(action: Action.RemoveMiddlePoint) {
        mapState.removeMarker(action.newSegment.centerId)
        addMarkerGrab(
            id = action.previousSegment.p2.markerId,
            x = action.previousSegment.p2.x,
            y = action.previousSegment.p2.y,
            color = { pointMarkerColor }
        )
        undoFuseSegments(
            previousToRestore = action.previousSegment,
            nextToRestore = action.nextSegment,
            oldToRemove = action.newSegment
        )
    }

    private fun undoFuseSegments(
        previousToRestore: TrackSegmentState,
        nextToRestore: TrackSegmentState,
        oldToRemove: TrackSegmentState
    ) {
        trackState.update {
            var hasReplaced = false
            buildList {
                for (s in it) {
                    if (s.id == oldToRemove.id && !hasReplaced) {
                        add(previousToRestore)
                        add(nextToRestore)
                        hasReplaced = true
                    } else {
                        add(s)
                    }
                }
            }
        }

        previousToRestore.p1.next = previousToRestore
        nextToRestore.p2.prev = nextToRestore

        configureCenterDrag(previousToRestore)
        configureCenterDrag(nextToRestore)
        mapState.removeMarker(previousToRestore.p2.markerId)
        addMarkerGrab(
            previousToRestore.p2.markerId,
            previousToRestore.p2.x,
            previousToRestore.p2.y,
            color = { pointMarkerColor }
        )
        configureDrag(previousToRestore.p2)
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
        var color by mutableStateOf(segmentCenterMarkerColor)
        var size by mutableStateOf(segmentCenterMarkerSize)
        addMarkerGrab(centerId, centerX, centerY, size = { size }, color = { color })
        var split: PointState? = null
        var dragStart: Point? = null
        mapState.enableMarkerDrag(
            centerId,
            onDragStart = { _, x, y ->
                if (split == null) {
                    size = pointMarkerSize
                    color = pointMarkerColor
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
                    addActionToUndoStack(
                        Action.MovePoint(
                            splitPoint,
                            from = from,
                            to = Point(x, y)
                        )
                    )
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

    private fun isCenterOfSegment(id: String): Boolean {
        return trackState.value.any { it.centerId == id }
    }

    private fun getPointState(id: String): PointState? {
        var found: PointState? = null
        for (s in trackState.value) {
            found = when {
                s.p1.markerId == id -> s.p1
                s.p2.markerId == id -> s.p2
                else -> continue
            }
        }
        return found
    }

    @OptIn(ExperimentalClusteringApi::class)
    private fun addMarkerGrab(
        id: String,
        x: Double,
        y: Double,
        size: () -> Dp = { pointMarkerSize },
        color: () -> Color
    ) {
        mapState.addMarker(
            id,
            x = x,
            y = y,
            relativeOffset = Offset(-0.5f, -0.5f),
            clickable = true,
            renderingStrategy = RenderingStrategy.Clustering(clustererId)
        ) {
            MarkerGrab(morphedIn = true, size = size(), color = color())
        }
    }

    private fun showStartToolTip(tooltipState: TooltipState, x: Double, y: Double) {
        mapState.addCallout(calloutId, x, y, absoluteOffset = Offset(0f, -pointMarkerSizePx / 2)) {
            StartToolTip(tooltipState)
        }
        scope.launch {
            tooltipState.show()
        }
    }

    private fun popUndo(): Action? {
        return undoStack.removeLastOrNull().also {
            hasUndoState.value = undoStack.size > 0
            lastUndoActionId.value = undoStack.lastOrNull()?.id
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
        lastUndoActionId.value = action.id
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

@Composable
private fun StartToolTip(tooltipState: TooltipState) {
    TooltipBox(
        state = tooltipState,
        positionProvider = rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(stringResource(id = R.string.track_create_start_title))
            }
        },
        content = {
        }
    )
}

private const val clustererId = "default"

private val startMarkerColor = Color(0x884CAF50)
private val pointMarkerColor = Color(0x55448AFF)
private val segmentCenterMarkerColor = Color(0x30000000)

private val pointMarkerSize = 50.dp
private val pointMarkerSizePx = dpToPx(pointMarkerSize.value)
private val segmentCenterMarkerSize = 35.dp
private const val calloutId = "delete-callout"

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
        return """PointState(x=$x, y=$y, next=${next?.id}, prev=${prev?.id}, markerId=$markerId)"""
    }
}

sealed class Action {
    val id = UUID.randomUUID().toString()

    data class AddPoint(val pointState: PointState) : Action()
    data class MovePoint(val pointState: PointState, val from: Point, val to: Point) : Action()
    data class SplitSegment(val previousSegment: TrackSegmentState, val newPoint: PointState) :
        Action()

    data class RemoveMiddlePoint(
        val previousSegment: TrackSegmentState,
        val nextSegment: TrackSegmentState,
        val newSegment: TrackSegmentState
    ) : Action()

    data class RemoveLastPoint(val pointState: PointState) : Action()
}
