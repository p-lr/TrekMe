package com.peterlaurence.trekme.features.map.presentation.ui.components

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.Path
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.util.lerp


@Composable
fun RecordingButtons(
    state: GpxRecordState,
    onStartStopClick: () -> Unit,
    onPauseResumeClick: () -> Unit
) {
    var scaleState by remember {
        mutableStateOf(
            if (state == GpxRecordState.STOPPED) 0f else 1f
        )
    }

    var firstTimeComposition by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = state) {
        if (firstTimeComposition) {
            firstTimeComposition = false
            return@LaunchedEffect
        }
        when (state) {
            GpxRecordState.STOPPED -> {
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                ) { value, _ -> scaleState = value }
            }

            GpxRecordState.STARTED -> {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(300) {
                        OvershootInterpolator().getInterpolation(it)
                    }
                ) { value, _ -> scaleState = value }
            }

            else -> { /* No anim */
            }
        }
    }

    Column(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MainRecordButton(
            isDestState = state == GpxRecordState.STOPPED,
            onClick = onStartStopClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        MorphingFab(
            Modifier.graphicsLayer {
                scaleX = scaleState
                scaleY = scaleState
            },
            isDestState = state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED,
            PathData(pausePath, Color(0xFFFFC107)),
            PathData(playPathDest, Color(0xFF4CAF50)),
            onClick = onPauseResumeClick
        )
    }
}

/* For pause <-> play */
private val pausePath =
    addPathNodes("M 17 31 L 17 17 L 21.66 17 L 21.66 31 M 26.33 31 L 26.33 17 L 31 17 L 31 31 Z")
private val playPathDest =
    addPathNodes("M 15 29 L 24 15 L 24 15 L 24 29 M 24 29 L 24 15 L 24 15 L 33 29 Z")

@Composable
private fun MainRecordButton(
    isDestState: Boolean,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        containerColor = Color.White,
        shape = CircleShape,
        onClick = onClick
    ) {
        if (isDestState) {
            Icon(
                painter = painterResource(id = R.drawable.record_rec),
                tint = Color(0xff666466),
                contentDescription = null
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.record_rec),
                tint = colorResource(id = R.color.colorAccentRed),
                contentDescription = null
            )
        }
    }
}

/**
 * A button which has two states. It animates when transitioning between states.
 * If a click happens in the middle of a transition, the state holder (typically a
 * view-model) decides whether the state changes or not.
 *
 * This button is useful when a component can be started or stopped, but click events are
 * debounced to avoid starting and stopping at a too high pace.
 */
@Composable
private fun MorphingFab(
    modifier: Modifier = Modifier,
    isDestState: Boolean,
    startPathData: PathData,
    destPathData: PathData,
    pathMorphingDurationMs: Int = 500,
    onClick: () -> Unit
) {
    val stateProgress = animateFloatAsState(
        if (isDestState) 0f else 1f,
        animationSpec = tween(pathMorphingDurationMs), label = "stateProgress"
    )
    val color by animateColorAsState(
        if (isDestState) startPathData.color else destPathData.color,
        animationSpec = tween(pathMorphingDurationMs), label = "color"
    )

    SmallFloatingActionButton(
        modifier = modifier,
        shape = CircleShape,
        containerColor = Color(
            red = opaqueEquivalent(0.2f, color.red),
            green = opaqueEquivalent(0.2f, color.green),
            blue = opaqueEquivalent(0.2f, color.blue)
        ),
        onClick = onClick
    ) {
        MorphingShape(
            modifier = modifier,
            size = 38.dp,
            startPath = startPathData.path,
            destPath = destPathData.path,
            color = color,
            t = stateProgress.value
        )
    }
}

/**
 * Context: a color is drawn with an alpha channel on a white background.
 * Given [alpha] and [color] as floats in the range 0f..1f, get the resulting color in the range
 * 0f..1f which is the full opaque equivalent.
 */
private fun opaqueEquivalent(alpha: Float, color: Float): Float {
    return 1f - alpha * (1f - color)
}

/**
 * Path morphs between two paths, and simultaneously rotates between 0° and 90°.
 */
@Composable
private fun MorphingShape(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    startPath: List<PathNode>,
    destPath: List<PathNode>,
    color: Color,
    t: Float
) {
    val pathNodes = lerp(startPath, destPath, t)
    val degree = t * 90

    Image(
        painter = rememberVectorPainter(
            defaultWidth = size,
            defaultHeight = size,
            viewportWidth = 48f,
            viewportHeight = 48f,
            autoMirror = false
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

private data class PathData(val path: List<PathNode>, val color: Color)

/* Doesn't seem to work in preview mode, but works when deployed on device. */
@Preview(showBackground = true)
@Composable
private fun RecordingButtonsPreview() {
    var state by remember { mutableStateOf(GpxRecordState.STOPPED) }


    TrekMeTheme {
        RecordingButtons(state,
            onStartStopClick = {
                state = if (state == GpxRecordState.STOPPED) {
                    GpxRecordState.STARTED
                } else {
                    GpxRecordState.STOPPED
                }
            },
            onPauseResumeClick = {
                state = if (state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED) {
                    GpxRecordState.PAUSED
                } else {
                    GpxRecordState.RESUMED
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
@ExperimentalAnimationApi
fun Preview0() {
    val transition: Transition<Boolean> = updateTransition(targetState = false, label = "")
    val t: Float by transition.animateFloat(
        transitionSpec = { spring(stiffness = 50f) }, label = ""
    ) { state ->
        if (state) 0f else 1f
    }
    TrekMeTheme {
        MorphingShape(Modifier, 48.dp, pausePath, playPathDest, Color.Blue, t)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview1() {
    TrekMeTheme {
        MorphingShape(Modifier, 48.dp, pausePath, playPathDest, Color.Blue, 0.25f)
    }
}


@Preview(showBackground = true)
@Composable
fun Preview2() {
    TrekMeTheme {
        MorphingShape(Modifier, 48.dp, pausePath, playPathDest, Color.Blue, 0.5f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    TrekMeTheme {
        MorphingShape(Modifier, 48.dp, pausePath, playPathDest, Color.Blue, 0.75f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview4() {
    TrekMeTheme {
        MorphingShape(Modifier, 48.dp, pausePath, playPathDest, Color.Blue, 1f)
    }
}
