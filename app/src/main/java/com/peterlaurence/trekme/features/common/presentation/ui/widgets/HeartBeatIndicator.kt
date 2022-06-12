package com.peterlaurence.trekme.features.common.presentation.ui.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.delay

@Composable
fun HeartBeatIndicator(
    isBeating: Boolean,
    innerRadius: Dp = 24.dp,
    outerRadius: Dp = 64.dp,
    beatingColor: Color = Color(0xfff44336),
    stoppedColor: Color = Color(0xff9e9e9e)
) {
    val animationValues = (0..1).map { index ->
        var animatedValue by remember { mutableStateOf(0f) }
        LaunchedEffect(isBeating) {
            if (!isBeating) return@LaunchedEffect
            delay(200L * index)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000),
                    repeatMode = RepeatMode.Restart,
                )
            ) { value, _ -> animatedValue = value }
        }
        animatedValue
    }

    Box(
        modifier = Modifier.size(outerRadius),
        contentAlignment = Alignment.Center
    ) {
        Circle(size = innerRadius, color = if (isBeating) beatingColor else stoppedColor)
        if (isBeating) {
            animationValues.forEach {
                Circle(
                    Modifier
                        .scale(1 + it * (outerRadius / innerRadius - 1))
                        .alpha(1 - it),
                    size = innerRadius,
                    color = beatingColor
                )
            }
        }
    }
}

@Composable
private fun Circle(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Gray,
) {
    Box(
        modifier = modifier
            .size(size)
            .clipToBounds()
            .background(color, CircleShape)
    )
}

@Preview
@Composable
private fun HeartBeatIndicatorPreview() {
    TrekMeTheme {
        HeartBeatIndicator(isBeating = true)
    }
}