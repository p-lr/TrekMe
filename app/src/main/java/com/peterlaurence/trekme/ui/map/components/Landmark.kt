package com.peterlaurence.trekme.ui.map.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.flow.Flow
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState

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
    distanceForIdFlow: Flow<Map<String, Double>>
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
    LaunchedEffect(landmarkPositions, positionMarker, distanceForId) {
        landmarkPositions.forEach { landmark ->
            val anchor = makeOffset(landmark.x, landmark.y, mapState)

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

                with(bubbleRectState.value) {
                    left = textRectState.value.left.toFloat() - offset + anchor.x
                    top = textRectState.value.top.toFloat() - offset + anchor.y
                    right = textRectState.value.right.toFloat() + offset + anchor.x
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
            drawLine(lineColor, start = pStart, end = pEnd, strokeWidth = 8f / mapState.scale)

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

private val distancePaint = Paint().apply {
    color = android.graphics.Color.WHITE
    textSize = dpToPx(12f)
    isAntiAlias = true
    style = Paint.Style.FILL
}

private val distanceTextBgPaint = Paint().apply {
    color = android.graphics.Color.parseColor("#CD9C27B0")
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