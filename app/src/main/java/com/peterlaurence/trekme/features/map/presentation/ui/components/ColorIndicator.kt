package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.util.parseColorL

@Composable
fun ColorIndicator(radius: Dp = 12.dp, color: String, onClick: () -> Unit = {}) {
    val colorContent = remember(color) {
        Color(parseColorL(color))
    }
    val background = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color.White
    Canvas(
        modifier = Modifier
            .size(radius * 2)
            .clickable(onClick = onClick)
    ) {
        val r = (radius - 2.dp).toPx()
        val r2 = radius.toPx()
        drawCircle(background, r2)
        drawCircle(colorContent, r)
    }
}