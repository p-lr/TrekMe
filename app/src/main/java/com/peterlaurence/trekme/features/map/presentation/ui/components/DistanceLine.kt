package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.peterlaurence.trekme.util.dpToPx
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.MarkerDataSnapshot
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun DistanceLine(
    modifier: Modifier = Modifier,
    mapState: MapState,
    marker1: MarkerDataSnapshot?,
    marker2: MarkerDataSnapshot?
) {
    if (marker1 == null || marker2 == null) return

    val pStart = remember(marker1) {
        makeOffset(marker1.x, marker1.y, mapState)
    }

    val pEnd = remember(marker2) {
        makeOffset(marker2.x, marker2.y, mapState)
    }

    DefaultCanvas(
        modifier = modifier,
        mapState = mapState
    ) {
        drawLine(
            lineColor,
            start = pStart,
            end = pEnd,
            strokeWidth = lineWidthPx / mapState.scale,
            cap = StrokeCap.Round
        )
    }
}

private val lineColor = Color(0xCC311B92)
private val lineWidthPx = dpToPx(4)