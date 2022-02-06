package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CalibrationMarker(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    sightWidth: Dp = 3.dp
) {

    val colorBg = remember { Color(0x55448AFF) }
    val colorSight = remember { Color(0xCC448AFF) }

    Canvas(
        modifier = modifier
            .size(size)
    ) {
        drawCircle(colorBg)
        drawLine(
            colorSight,
            Offset(0f, size.toPx() / 2),
            Offset(size.toPx() / 2 - sightWidth.toPx() / 2, size.toPx() / 2),
            strokeWidth = sightWidth.toPx()
        )
        drawLine(
            colorSight,
            Offset(size.toPx() / 2 + sightWidth.toPx() / 2, size.toPx() / 2),
            Offset(size.toPx(), size.toPx() / 2),
            strokeWidth = sightWidth.toPx()
        )
        drawLine(
            colorSight,
            Offset(size.toPx() / 2, 0f),
            Offset(size.toPx() / 2, size.toPx() / 2 - sightWidth.toPx() / 2),
            strokeWidth = sightWidth.toPx()
        )
        drawLine(
            colorSight,
            Offset(size.toPx() / 2, size.toPx() / 2 + sightWidth.toPx() / 2),
            Offset(size.toPx() / 2, size.toPx()),
            strokeWidth = sightWidth.toPx()
        )
    }
}