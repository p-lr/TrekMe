package com.peterlaurence.trekme.features.record.presentation.ui.components

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.m3.TrekMeTheme
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.features.record.presentation.ui.components.widgets.MorphingButton
import com.peterlaurence.trekme.features.record.presentation.ui.components.widgets.MorphingShape
import com.peterlaurence.trekme.features.record.presentation.ui.components.widgets.PathData
import com.peterlaurence.trekme.features.record.presentation.viewmodel.GpxRecordServiceViewModel

@Composable
fun ActionsStateful(
    modifier: Modifier = Modifier,
    viewModel: GpxRecordServiceViewModel,
    onStartStopClick: () -> Unit,
    onPauseResumeClick: () -> Unit
) {
    val gpxRecordState by viewModel.status.collectAsState()

    Actions(
        modifier = modifier,
        gpxRecordState = gpxRecordState,
        onStartStopClick = onStartStopClick,
        onPauseResumeClick = onPauseResumeClick
    )
}

@Composable
private fun Actions(
    modifier: Modifier = Modifier,
    gpxRecordState: GpxRecordState,
    onStartStopClick: () -> Unit,
    onPauseResumeClick: () -> Unit
) {
    Card(modifier) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(id = R.string.control_card_title),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(id = R.string.control_card_subtitle),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PlayPauseStop(gpxRecordState, onStartStopClick, onPauseResumeClick)
            }
        }
    }
}


/* For play <-> stop */
private val playPath = addPathNodes("M 19 33 L 19 15 L 33 24 L 33 24 Z")
private val stopPath = addPathNodes("M 17 31 L 17 17 L 31 17 L 31 31 Z")

/* For pause <-> play */
private val pausePath =
    addPathNodes("M 17 31 L 17 17 L 21.66 17 L 21.66 31 M 26.33 31 L 26.33 17 L 31 17 L 31 31 Z")
private val playPathDest =
    addPathNodes("M 15 29 L 24 15 L 24 15 L 24 29 M 24 29 L 24 15 L 24 15 L 33 29 Z")

@Composable
private fun PlayPauseStop(
    state: GpxRecordState,
    onStartStopClick: () -> Unit,
    onPauseResumeClick: () -> Unit
) {
    var animatedValue by remember {
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
                ) { value, _ -> animatedValue = value }
            }
            GpxRecordState.STARTED -> {
                animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = tween(300) {
                        OvershootInterpolator().getInterpolation(it)
                    }
                ) { value, _ -> animatedValue = value }
            }
            else -> { /* No anim */
            }
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        MorphingButton(
            Modifier.size(48.dp),
            isDestState = state == GpxRecordState.STOPPED,
            PathData(playPath, Color(0xFF4CAF50)),
            PathData(stopPath, Color(0xFFF44336)),
            onClick = {
                onStartStopClick()
            }
        )
        Spacer(modifier = Modifier.width((30 * animatedValue).dp))

        MorphingButton(
            Modifier.size(48.dp * animatedValue),
            isDestState = state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED,
            PathData(pausePath, Color(0xFFFFC107)),
            PathData(playPathDest, Color(0xFF4CAF50)),
            showTimeout = false,
            onClick = onPauseResumeClick
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StartStopPreview() {
    var state by remember { mutableStateOf(GpxRecordState.STOPPED) }
    TrekMeTheme {
        PlayPauseStop(state,
            onStartStopClick = {
                state =
                    if (state == GpxRecordState.STOPPED) GpxRecordState.STARTED else GpxRecordState.STOPPED
            },
            onPauseResumeClick = {
                state =
                    if (state == GpxRecordState.PAUSED) GpxRecordState.RESUMED else GpxRecordState.PAUSED
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
        MorphingShape(Modifier, pausePath, playPathDest, Color.Blue, t)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview1() {
    TrekMeTheme {
        MorphingShape(Modifier, pausePath, playPathDest, Color.Blue, 0.25f)
    }
}


@Preview(showBackground = true)
@Composable
fun Preview2() {
    TrekMeTheme {
        MorphingShape(Modifier, pausePath, playPathDest, Color.Blue, 0.5f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview3() {
    TrekMeTheme {
        MorphingShape(Modifier, pausePath, playPathDest, Color.Blue, 0.75f)
    }
}

@Preview(showBackground = true)
@Composable
fun Preview4() {
    TrekMeTheme {
        MorphingShape(Modifier, pausePath, playPathDest, Color.Blue, 1f)
    }
}