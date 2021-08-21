package com.peterlaurence.trekme.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun PositionMarker(modifier: Modifier = Modifier) {
    val color = colorResource(id = R.color.colorPositionMarker)
    Canvas(modifier = modifier.size(60.dp)) {
        drawCircle(color, 7.5.dp.toPx())
        drawCircle(color, 14.dp.toPx(), alpha = 0.55f)
    }
}