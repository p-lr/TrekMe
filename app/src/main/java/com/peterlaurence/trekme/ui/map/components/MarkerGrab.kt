package com.peterlaurence.trekme.ui.map.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MarkerGrab(
    modifier: Modifier = Modifier.size(100.dp),
    morphedIn: Boolean,
    onMorphOutDone: () -> Unit = {}
) {
    val color = remember { Color(0x55448AFF) }
    Canvas(modifier = modifier) {
        drawCircle(color)
    }
}