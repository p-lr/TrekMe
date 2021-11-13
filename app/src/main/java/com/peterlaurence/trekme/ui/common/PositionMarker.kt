package com.peterlaurence.trekme.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun PositionMarker(modifier: Modifier = Modifier) {
    val color = colorResource(id = R.color.colorPositionMarker)
    Canvas(modifier = modifier.size(29.dp)) {
        drawCircle(color, 7.5.dp.toPx())
        drawCircle(color, 14.dp.toPx(), alpha = 0.33f)
    }
}

@Composable
fun PositionOrientationMarker(modifier: Modifier = Modifier, angle: Float? = null) {
    val color = colorResource(id = R.color.colorPositionMarker)
    val innerRadius = with(LocalDensity.current) { 10.75.dp.toPx() }
    val outerRadius = with(LocalDensity.current) { 30.dp.toPx() }

    val arrowPath = remember { makeArrowPath(innerRadius, outerRadius) }

    Box(
        modifier = modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        PositionMarker()
        if (angle != null) {
            Canvas(modifier.rotate(angle)) {
                drawPath(arrowPath, color)
            }
        }
    }
}

private fun makeArrowPath(innerRadius: Float, outerRadius: Float) = Path().apply {
    moveTo(0f, -innerRadius)
    arcTo(Rect(Offset.Zero, innerRadius), -90f, 33f, false)
    lineTo(0f, -outerRadius)
    arcTo(Rect(Offset.Zero, innerRadius), -90f, -33f, false)
    lineTo(0f, -outerRadius)
}

@Preview
@Composable
private fun PositionMarkerPreview() {
    PositionMarker()
}

@Preview
@Composable
private fun PositionOrientationMarkerPreview() {
    PositionOrientationMarker(angle = 45f)
}