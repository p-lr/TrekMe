package com.peterlaurence.trekme.ui.map.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.MarkerDataSnapshot
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.api.scale
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
    landmarkPositions: List<MarkerDataSnapshot>
) {
    if (positionMarker != null) {
        val pStart = makeOffset(positionMarker.x, positionMarker.y, mapState)

        DefaultCanvas(
            modifier = modifier,
            mapState = mapState
        ) {
            for (l in landmarkPositions) {
                val pEnd = makeOffset(l.x, l.y, mapState)
                drawLine(lineColor, start = pStart, end = pEnd, strokeWidth = 8f / mapState.scale)
            }
        }
    }
}


private fun makeOffset(x: Double, y: Double, mapState: MapState): Offset {
    return Offset(
        x = (mapState.fullSize.width * x).toFloat(),
        y = (mapState.fullSize.height * y).toFloat()
    )
}

private val color = Color(0xFF9C27B0)
private val strokeColor = Color(0xFF4A148C)
private val lineColor = Color(0xCD9C27B0)