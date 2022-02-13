package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.util.pxToDp

@Composable
fun ScaleIndicator(
    widthPx: Int,
    widthRatio: Float,
    scaleText: String,
    lineColor: Color
) {
    Box(Modifier.height(50.dp)) {
        Canvas(
            modifier = Modifier
                .alpha(0.8f)
                .padding(5.dp)
                .size(pxToDp(widthPx).dp, 15.dp)
        ) {
            val width = widthPx * widthRatio
            val height = size.height
            drawLine(lineColor, Offset(0f, height / 2), Offset(width, height / 2), 2.dp.toPx())
            drawLine(
                lineColor,
                Offset(0f, 0f),
                Offset(0f, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                lineColor,
                Offset(width, 0f),
                Offset(width, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = scaleText,
            color = Color.White,
            modifier = Modifier
                .padding(start = 16.dp, top = 20.dp)
                .background(color = Color(0x885D4037), shape = RoundedCornerShape(4.dp))
                .padding(start = 5.dp, end = 5.dp)
        )
    }
}
