package com.peterlaurence.trekme.ui.record.components.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.ui.record.RecordFragment
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.lerp
import com.peterlaurence.trekme.viewmodel.GpxRecordServiceViewModel
import kotlinx.coroutines.launch

/**
 * A button which has two states (started, and stopped). It animates when transitioning between
 * states. If a click happens in the middle of a transition, the state holder (typically a
 * view-model) decides whether the state changes or not.
 *
 * This button is useful when a component can be started or stopped, but click events are
 * debounced to avoid starting and stopping at a too high pace.
 *
 * This button carries two information:
 * * The state started/stopped,
 * * The state transition, during which the button cannot change of state.
 */
@Composable
fun StartStopButton(
        modifier: Modifier = Modifier,
        stopped: Boolean,
        startPathData: PathData,
        destPathData: PathData,
        pathMorphingDurationMs: Int = 500,
        disableTimeoutMs: Int = 2000,
        onClick: () -> Unit
) {
    val animatedProgress = animateFloatAsState(
            if (stopped) 0f else 1f,
            animationSpec = tween(pathMorphingDurationMs)
    )
    val backgroundColor by animateColorAsState(
            if (stopped) startPathData.color else destPathData.color,
            animationSpec = tween(pathMorphingDurationMs)
    )
    val strokeColor = if (stopped) {
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
     * Everytime the state [stopped] changes, the timeout animation should stop and restart. This is
     * exactly the purpose of [LaunchedEffect].
     * There's an exception though. When the button is composed for the first time, we don't want to
     * see the timeout animation.
     */
    var firstTimeComposition by remember { mutableStateOf(true) }
    LaunchedEffect(stopped) {
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
                    .clickable { onClick() }
    ) {
        TimeoutShape(
                modifier = modifier,
                backgroundColor,
                strokeColor,
                t = progress
        )
        StartStopShape(
                modifier = modifier,
                startPathData.path,
                destPathData.path,
                backgroundColor,
                t = animatedProgress.value
        )
    }
}

@Composable
fun TimeoutShape(
        modifier: Modifier,
        backgroundColor: Color,
        strokeColor: Color,
        t: Float
) {
    val angle = if (t != 0f && t != 1f) {
        t * 360f
    } else 0f

    Canvas(
            modifier = modifier
                    .background(backgroundColor.copy(alpha = 0.2f))
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
        backgroundColor: Color,
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
                        fill = SolidColor(backgroundColor)
                )
            },
            modifier = modifier.rotate(degree),
            contentDescription = null
    )
}

/* For play <-> stop */
private val playPath = addPathNodes("M 19 33 L 19 15 L 33 24 L 33 24 Z")
private val stopPath = addPathNodes("M 17 31 L 17 17 L 31 17 L 31 31 Z")

/* For pause <-> play */
private val pausePath = addPathNodes("M 17 31 L 17 17 L 21.66 17 L 21.66 31 M 26.33 31 L 26.33 17 L 31 17 L 31 31 Z")
private val playPathDest = addPathNodes("M 15 29 L 24 15 L 24 15 L 24 29 M 24 29 L 24 15 L 24 15 L 33 29 Z")

data class PathData(val path: List<PathNode>, val color: Color)

@Preview(showBackground = true)
@Composable
fun Preview0() {
    TrekMeTheme {
        StartStopShape(Modifier, pausePath, playPathDest, Color.Blue, 0f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview1() {
    TrekMeTheme {
        StartStopShape(Modifier, pausePath, playPathDest, Color.Blue, 0.25f)
    }
}


@Preview(showBackground = true)
@Composable
fun Preview2() {
    TrekMeTheme {
        StartStopShape(Modifier, pausePath, playPathDest, Color.Blue, 0.5f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    TrekMeTheme {
        StartStopShape(Modifier, pausePath, playPathDest, Color.Blue, 0.75f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview4() {
    TrekMeTheme {
        StartStopShape(Modifier, pausePath, playPathDest, Color.Blue, 1f)
    }
}

class StartStopButtonView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpxRecordServiceViewModel = viewModel(findFragment<RecordFragment>().requireActivity())
        val started by viewModel.status.collectAsState()

        TrekMeTheme {
            StartStopButton(
                Modifier.size(48.dp),
                stopped = !started,
                PathData(playPath, Color(0xFF4CAF50)),
                PathData(stopPath, Color(0xFFF44336)),
                onClick = viewModel::onStartStopClicked)
        }
    }
}