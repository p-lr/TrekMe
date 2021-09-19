package com.peterlaurence.trekme.ui.record.components.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.util.lerp
import kotlinx.coroutines.launch

/**
 * A button which has two states. It animates when transitioning between states.
 * If a click happens in the middle of a transition, the state holder (typically a
 * view-model) decides whether the state changes or not.
 *
 * This button is useful when a component can be started or stopped, but click events are
 * debounced to avoid starting and stopping at a too high pace.
 *
 * This button carries two information:
 * * The state start state / dest state,
 * * The state transition, during which the button cannot change of state.
 */
@Composable
fun TwoStateButton(
    modifier: Modifier = Modifier,
    isDestState: Boolean,
    startPathData: PathData,
    destPathData: PathData,
    pathMorphingDurationMs: Int = 500,
    disableTimeoutMs: Int = 2000,
    showTimeout: Boolean = true,
    onClick: () -> Unit
) {
    val animatedProgress = animateFloatAsState(
        if (isDestState) 0f else 1f,
        animationSpec = tween(pathMorphingDurationMs)
    )
    val color by animateColorAsState(
        if (isDestState) startPathData.color else destPathData.color,
        animationSpec = tween(pathMorphingDurationMs)
    )
    val strokeColor = if (isDestState) {
        startPathData.color
    } else {
        destPathData.color
    }

    val anim = remember {
        TargetBasedAnimation(
            animationSpec = tween(disableTimeoutMs),
            typeConverter = Float.VectorConverter,
            initialValue = 0f,
            targetValue = 1f
        )
    }

    var progress by remember { mutableStateOf(0f) }

    /**
     * Everytime the state [isDestState] changes, the timeout animation should stop and restart. This is
     * exactly the purpose of [LaunchedEffect].
     * There's an exception though. When the button is composed for the first time, we don't want to
     * see the timeout animation.
     */
    var firstTimeComposition by remember { mutableStateOf(true) }
    LaunchedEffect(isDestState) {
        if (firstTimeComposition) {
            firstTimeComposition = false
            return@LaunchedEffect
        }
        launch {
            val startTime = withFrameNanos { it }

            do {
                val playTime = withFrameNanos { it } - startTime
                progress = anim.getValueFromNanos(playTime)
            } while (progress < 1f)
        }
    }

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() },
        color = color.copy(alpha = 0.2f)
    ) {
        if (showTimeout) {
            TimeoutShape(
                modifier = modifier,
                strokeColor,
                t = progress
            )
        }
        StartStopShape(
            modifier = modifier,
            startPathData.path,
            destPathData.path,
            color,
            t = animatedProgress.value
        )
    }
}

@Composable
fun TimeoutShape(
    modifier: Modifier,
    strokeColor: Color,
    t: Float
) {
    val angle = if (t != 0f && t != 1f) {
        t * 360f
    } else 0f

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(1.5.dp),
        ) {
        drawArc(
            color = strokeColor,
            startAngle = -90f,
            sweepAngle = angle,
            useCenter = false,
            size = Size(size.width, size.height),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
fun StartStopShape(
    modifier: Modifier = Modifier,
    startPath: List<PathNode>,
    destPath: List<PathNode>,
    color: Color,
    t: Float
) {
    val pathNodes = lerp(startPath, destPath, t)
    val degree = t * 90

    Image(
        painter = rememberVectorPainter(
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ) { _, _ ->
            Path(
                pathData = pathNodes,
                fill = SolidColor(color)
            )
        },
        modifier = modifier.rotate(degree),
        contentDescription = null
    )
}

data class PathData(val path: List<PathNode>, val color: Color)
