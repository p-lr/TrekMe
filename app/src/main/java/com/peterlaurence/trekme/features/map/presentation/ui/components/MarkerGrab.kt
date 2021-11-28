package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MarkerGrab(
    modifier: Modifier = Modifier,
    morphedIn: Boolean,
    size: Dp = 100.dp,
    onMorphOutDone: () -> Unit = {}
) {
    /* We need to control the initial value, so we can't use animateFloatAsState */
    val animatedScale = remember { Animatable(0f) }
    LaunchedEffect(morphedIn) {
        animatedScale.animateTo(if (morphedIn) 1f else 0f, tween(500))
        if (!morphedIn) onMorphOutDone()
    }

    val color = remember { Color(0x55448AFF) }
    Canvas(
        modifier = modifier
            .size(size * animatedScale.value)
    ) {
        drawCircle(color)
    }
}