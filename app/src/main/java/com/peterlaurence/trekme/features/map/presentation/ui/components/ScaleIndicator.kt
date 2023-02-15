package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.util.pxToDp

@Composable
fun ScaleIndicator(
    widthPx: Int,
    widthRatio: Float,
    scaleText: String,
    color: Color = MaterialTheme.colorScheme.tertiaryContainer
) {
    Box(
        Modifier.height(25.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(
            modifier = Modifier
                .alpha(0.8f)
                .padding(horizontal = 5.dp)
                .size(pxToDp(widthPx).dp, 15.dp)
        ) {
            val width = widthPx * widthRatio
            val height = size.height
            drawLine(Color.Black, Offset(0f, height / 2), Offset(width, height / 2), 2.dp.toPx())
            drawLine(
                Color.Black,
                Offset(0f, 0f),
                Offset(0f, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                Color.Black,
                Offset(width, 0f),
                Offset(width, height),
                2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Text(
            text = scaleText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .padding(start = 16.dp)
                .background(color = color, shape = RoundedCornerShape(4.dp))
                .padding(start = 5.dp, end = 5.dp)
        )
    }
}
