package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

/**
 * A beacon represents an area on a map.
 * The intended usage is to trigger a vibration (and/or play a sound) when the user enters the area.
 * This composable has two modes: static or not. When in static form, a dashed circle slowly rotates
 * around the beacon center.
 *
 * @param beaconVicinityRadiusPx The radius of the beacon at scale 1. This quantity should be
 * computed beforehand. For example, when rendered at scale 1, the beacon represents an area with a
 * radius of 50m.
 */
@Composable
fun Beacon(
    modifier: Modifier = Modifier,
    animationDurationMs: Int = 1500,
    beaconVicinityRadiusPx: Float,
    scale: Float = 1f,
    isStatic: Boolean = true
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(isStatic) {
        animatedProgress.animateTo(if (isStatic) 0f else 1f, animationSpec = tween(animationDurationMs))
    }

    val radius = beaconVicinityRadiusPx * scale
    val sizeDp = with(LocalDensity.current) {
        (radius * 2).toDp() + 2.dp
    }

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(29.dp)) {
            drawCircle(color, 5.dp.toPx())
            drawCircle(color, radius * (1 - animatedProgress.value), alpha = 0.2f)
        }

        if (animatedProgress.value == 0f) {
            val infiniteTransition = rememberInfiniteTransition()
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            val dashSize = with(LocalDensity.current) {
                6.dp.toPx()
            }

            val pathEffect = remember {
                PathEffect.dashPathEffect(floatArrayOf(dashSize, dashSize), 0f)
            }

            Canvas(modifier = Modifier
                .size(sizeDp)
                .rotate(angle)) {
                drawCircle(strokeColor, radius, style = Stroke(width = 1.dp.toPx(), pathEffect = pathEffect))
            }
        }
    }
}

private val color = Color(0xFF9C27B0)
private val strokeColor = Color(0xFF4A148C)

@Preview(widthDp = 250)
@Composable
fun BeaconPreview() {
    TrekMeTheme {
        var isStatic by remember { mutableStateOf(true) }
        var scale by remember { mutableStateOf(1f) }
        var radius by remember { mutableStateOf(100f) }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { isStatic = !isStatic }) {
                Text("Toggle")
            }
            Slider(value = scale, onValueChange = { scale = it }, valueRange = 0f..2f)
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 100f..200f)
            Beacon(Modifier.size(100.dp), beaconVicinityRadiusPx = radius, scale = scale, isStatic = isStatic)
        }
    }
}