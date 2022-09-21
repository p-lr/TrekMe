package com.peterlaurence.trekme.features.map.presentation.ui.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.util.lerp

@Composable
fun Marker(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFF16157),
    strokeColor: Color = Color(0xFFB6392F),
    animationDurationMs: Int = 500,
    isStatic: Boolean = true
) {
    val animatedProgress = animateFloatAsState(
        if (isStatic) 0f else 1f,
        animationSpec = tween(animationDurationMs)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        MarkerShape(
            backgroundColor = backgroundColor,
            strokeColor = strokeColor,
            t = animatedProgress.value
        )
        if (animatedProgress.value == 1f) {
            val infiniteTransition = rememberInfiniteTransition()
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            ArrowsShape(
                modifier = modifier,
                backgroundColor = strokeColor,
                angle = angle
            )
        }
    }
}

@Composable
fun MarkerShape(
    backgroundColor: Color,
    strokeColor: Color,
    t: Float
) {
    val staticPath =
        remember { addPathNodes(markerSvgPath) }
    val dynamicPath =
        remember { addPathNodes(roundSvgPath) }

    val pathNodes = lerp(staticPath, dynamicPath, t)

    Image(
        painter = rememberVectorPainter(
            defaultWidth = 24.dp,
            defaultHeight = 48.dp,
            viewportWidth = 24f,
            viewportHeight = 48f,
            autoMirror = false
        ) { _, _ ->
            Path(
                pathData = pathNodes,
                fill = SolidColor(backgroundColor),
                stroke = SolidColor(strokeColor),
                strokeLineWidth = 1f
            )
        },
        contentDescription = null
    )
}

@Composable
fun ArrowsShape(
    modifier: Modifier,
    backgroundColor: Color,
    angle: Float
) {
    val leftArrowPath = remember { addPathNodes("M 3,26 v -4 l -3,2 Z") }
    val rightArrowPath = remember { addPathNodes("M 21,26 v -4 l 3,2 Z") }
    val topArrowPath = remember { addPathNodes("M 10,15 h 4 l -2,-3 Z") }
    val bottomArrowPath = remember { addPathNodes("M 10,33 h 4 l -2,3 Z") }

    Image(
        painter = rememberVectorPainter(
            defaultWidth = 24.dp,
            defaultHeight = 48.dp,
            viewportWidth = 24f,
            viewportHeight = 48f,
            autoMirror = false
        ) { _, _ ->
            Path(
                pathData = leftArrowPath,
                fill = SolidColor(backgroundColor),
            )
            Path(
                pathData = rightArrowPath,
                fill = SolidColor(backgroundColor),
            )
            Path(
                pathData = topArrowPath,
                fill = SolidColor(backgroundColor),
            )
            Path(
                pathData = bottomArrowPath,
                fill = SolidColor(backgroundColor),
            )
        },
        modifier = modifier.rotate(angle),
        contentDescription = null
    )
}

private const val markerSvgPath =
    "M12,11.5c-1.381,0-2.5-1.119-2.5-2.5s1.119-2.5,2.5-2.5s2.5,1.119,2.5,2.5s-1.119,2.5,-2.5,2.5 M12,2c-3.866,0,-7,3.134,-7,7c0,5.25,7,13,7,13s7-7.75,7-13c0,-3.686,-3.314,-7,-7,-7z"
private const val roundSvgPath =
    "M12,26.5c-1.381,0-2.5-1.119-2.5-2.5s1.119-2.5,2.5-2.5s2.5,1.119,2.5,2.5s-1.119,2.5,-2.5,2.5 M12,17c-3.866,0,-7,3.134,-7,7c0,3.866,3.314,7,7,7s7-3.314,7-7c0,-3.686,-3.314,-7,-7,-7z"