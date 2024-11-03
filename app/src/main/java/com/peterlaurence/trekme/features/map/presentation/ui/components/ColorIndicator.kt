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
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.util.parseColorL

@Composable
fun ColorIndicator(color: String, onClick: () -> Unit = {}) {
    val colorContent = remember(color) {
        Color(parseColorL(color))
    }
    val background = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color.White
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .clickable(onClick = onClick)
    ) {
        val r = 10.dp.toPx()
        val r2 = 12.dp.toPx()
        drawCircle(background, r2)
        drawCircle(colorContent, r)
    }
}