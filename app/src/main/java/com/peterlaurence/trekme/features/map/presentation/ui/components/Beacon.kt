package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

/**
 * A beacon represents an area on a map.
 * The intended usage is to trigger a vibration (and/or play a sound) when the user enters the area.
 * This composable has two modes: static or not. When in static form, it shows a dashed circle.
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
        animatedProgress.animateTo(
            if (isStatic) 0f else 1f,
            animationSpec = tween(animationDurationMs)
        )
    }

    val radius = beaconVicinityRadiusPx * scale
    val sizeDp = with(LocalDensity.current) {
        (radius * 2).toDp() + 2.dp
    }

    val dashSize = with(LocalDensity.current) {
        6.dp.toPx()
    }

    val pathEffect = remember {
        PathEffect.dashPathEffect(floatArrayOf(dashSize, dashSize), 0f)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .drawBehind {
                drawCircle(color, 4.dp.toPx(), alpha = 0.7f)
                drawCircle(color, radius * (1 - animatedProgress.value), alpha = 0.2f)
                if (animatedProgress.value == 0f) {
                    drawCircle(
                        strokeColor,
                        radius,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = pathEffect)
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {}
}

/**
 * Represents the clickable area of a beacon. It has a fixed size.
 */
@Composable
fun BeaconClickArea() {
    BoxWithConstraints(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier) {
            drawCircle(
                color = color,
                alpha = 0.3f,
                radius = maxWidth.toPx() / 2,
                style = Fill
            )
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
            Beacon(
                Modifier.size(100.dp),
                beaconVicinityRadiusPx = radius,
                scale = scale,
                isStatic = isStatic
            )
        }
    }
}