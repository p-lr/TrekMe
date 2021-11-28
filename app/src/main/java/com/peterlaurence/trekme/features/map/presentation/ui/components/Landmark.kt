package com.peterlaurence.trekme.features.map.presentation.ui.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.flow.Flow
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.abs

/**
 * A landmark is just a [Marker] with different colors.
 */
@Composable
fun LandMark(
    modifier: Modifier = Modifier,
    isStatic: Boolean
) = Marker(
    modifier,
    backgroundColor = color,
    strokeColor = strokeColor,
    isStatic = isStatic
)

@Composable
fun LandmarkLines(
    modifier: Modifier = Modifier,
    mapState: MapState,
    positionMarker: MarkerDataSnapshot?,
    landmarkPositions: List<MarkerDataSnapshot>,
    distanceForIdFlow: Flow<Map<String, Double?>>
) {
    if (positionMarker == null) return

    val distanceForId by distanceForIdFlow.collectAsState(initial = mapOf())

    val labelDataForId = remember {
        mutableStateMapOf<String, LabelData>()
    }

    val offset = remember {
        dpToPx(4f)
    }

    /* Compute the position and size of distance labels. */
    LaunchedEffect(
        landmarkPositions,
        positionMarker,
        distanceForId,
        mapState.scale,
        mapState.scroll
    ) {
        landmarkPositions.forEach { landmark ->
            val boundingBox = mapState.visibleArea()
            val anchor = coerceInBoundingBox(
                mapState, boundingBox, positionMarker.x, positionMarker.y,
                landmark.x, landmark.y
            ) ?: makeOffset(
                (landmark.x + positionMarker.x) / 2,
                (landmark.y + positionMarker.y) / 2,
                mapState
            )

            if (labelDataForId.containsKey(landmark.id)) {
                labelDataForId[landmark.id]?.apply {
                    anchorState.value = anchor
                }
            } else {
                labelDataForId[landmark.id] = LabelData(
                    anchorState = mutableStateOf(anchor),
                    distanceState = mutableStateOf(null),
                    textRectState = mutableStateOf(Rect()),
                    bubbleRectState = mutableStateOf(RectF())
                )
            }
            labelDataForId[landmark.id]?.apply {
                val distance = distanceForId[landmark.id] ?: return@apply
                val distStr = formatDistance(distance)
                distanceState.value = distStr
                distancePaint.getTextBounds(distStr, 0, distStr.length, textRectState.value)

                val textWidth = textRectState.value.right - textRectState.value.left
                with(bubbleRectState.value) {
                    left = textRectState.value.left.toFloat() - offset + anchor.x - textWidth / 2
                    top = textRectState.value.top.toFloat() - offset + anchor.y
                    right = textRectState.value.right.toFloat() + offset + anchor.x - textWidth / 2
                    bottom = textRectState.value.bottom.toFloat() + offset + anchor.y
                }
            }
        }
    }

    val pStart = remember(positionMarker) {
        makeOffset(positionMarker.x, positionMarker.y, mapState)
    }

    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        for (l in landmarkPositions) {
            val pEnd = makeOffset(l.x, l.y, mapState)
            drawLine(
                lineColor,
                start = pStart,
                end = pEnd,
                strokeWidth = lineWidthPx / mapState.scale,
                cap = StrokeCap.Round
            )

            val distanceLabel = labelDataForId[l.id] ?: continue
            val dist = distanceLabel.distanceState.value ?: continue

            val anchor = distanceLabel.anchorState.value
            drawIntoCanvas {
                scale(1 / mapState.scale, anchor) {
                    rotate(-mapState.rotation, anchor) {
                        it.nativeCanvas.drawRoundRect(
                            distanceLabel.bubbleRectState.value, 6f, 6f, distanceTextBgPaint
                        )
                        it.nativeCanvas.drawText(dist, anchor.x, anchor.y, distancePaint)
                    }
                }
            }
        }
    }
}

private fun coerceInBoundingBox(
    mapState: MapState,
    bb: VisibleArea,
    aX: Double,
    aY: Double,
    bX: Double,
    bY: Double
): Offset? {
    if (aX == bX || aY == bY) return null

    fun VisibleArea.contains(x: Double, y: Double): Boolean {
        fun triangleArea(x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Double {
            return abs((x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2)) / 2.0)
        }

        val fullArea = triangleArea(p1x, p1y, p2x, p2y, p3x, p3y) + triangleArea(p1x, p1y, p4x, p4y, p3x, p3y)
        val t1 = triangleArea(x, y, p1x, p1y, p2x, p2y)
        val t2 = triangleArea(x, y, p2x, p2y, p3x, p3y)
        val t3 = triangleArea(x, y, p3x, p3y, p4x, p4y)
        val t4 = triangleArea(x, y, p1x, p1y, p4x, p4y)

        return abs(fullArea - (t1 + t2 + t3 + t4)) < 1E-8
    }

    /* If the two points are visible, return the middle */
    if (bb.contains(aX, aY) && bb.contains(bY, bY)) return makeOffset(
        (aX + bX) / 2,
        (aY + bY) / 2,
        mapState
    )

    fun findIntersect(
        l1X: Double,
        l1Y: Double,
        l2X: Double,
        l2Y: Double,
        intersects: MutableList<Double>
    ): Boolean {
        /* Find x intersection */
        val s1X = bX - aX
        val s2X = l2X - l1X
        val s1Y = bY - aY
        val s2Y = l2Y - l1Y

        val s = (-s1Y * (aX - l1X) + s1X * (aY - l1Y)) / (-s2X * s1Y + s1X * s2Y)
        val t = (s2X * (aY - l1Y) - s2Y * (aX - l1X)) / (-s2X * s1Y + s1X * s2Y)

        return if (s in 0.0..1.0 && t in 0.0..1.0) {
            intersects.add(aX + t * s1X)
            intersects.add(aY + t * s1Y)
            true
        } else false
    }

    val intersections = mutableListOf<Double>()

    findIntersect(bb.p1x, bb.p1y, bb.p2x, bb.p2y, intersections)
    findIntersect(bb.p2x, bb.p2y, bb.p3x, bb.p3y, intersections)
    findIntersect(bb.p3x, bb.p3y, bb.p4x, bb.p4y, intersections)
    findIntersect(bb.p4x, bb.p4y, bb.p1x, bb.p1y, intersections)

    if (intersections.size == 0) return null
    if (intersections.size == 2) {
        if (bb.contains(aX, aY)) {
            return makeOffset(
                (aX + intersections[0]) / 2,
                (aY + intersections[1]) / 2,
                mapState
            )
        } else if (bb.contains(bX, bY)) {
            return makeOffset(
                (bX + intersections[0]) / 2,
                (bY + intersections[1]) / 2,
                mapState
            )
        } else return null
    }
    if (intersections.size == 4) {
        return makeOffset(
            (intersections[0] + intersections[2]) / 2,
            (intersections[1] + intersections[3]) / 2,
            mapState
        )
    }
    return null
}

private val distancePaint = Paint().apply {
    color = android.graphics.Color.WHITE
    textSize = dpToPx(12f)
    isAntiAlias = true
    style = Paint.Style.FILL
    textAlign = Paint.Align.CENTER
}

private val distanceTextBgPaint = Paint().apply {
    color = android.graphics.Color.parseColor("#CD9C27B0")  // full opaque equiv is #b052c0
    isAntiAlias = true
    style = Paint.Style.FILL
}

private fun makeOffset(x: Double, y: Double, mapState: MapState): Offset {
    return Offset(
        x = (mapState.fullSize.width * x).toFloat(),
        y = (mapState.fullSize.height * y).toFloat()
    )
}

private data class LabelData(
    val anchorState: MutableState<Offset>,
    val distanceState: MutableState<String?>,
    val textRectState: MutableState<Rect>,
    val bubbleRectState: MutableState<RectF>
)

private val color = Color(0xFF9C27B0)
private val strokeColor = Color(0xFF4A148C)
private val lineColor = Color(0xCD9C27B0)
private val lineWidthPx = dpToPx(4)