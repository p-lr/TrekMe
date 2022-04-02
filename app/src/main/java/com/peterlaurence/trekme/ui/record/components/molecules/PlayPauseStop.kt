package com.peterlaurence.trekme.ui.record.components.molecules

import android.content.Context
import android.util.AttributeSet
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.service.GpxRecordState
import com.peterlaurence.trekme.ui.record.RecordFragment
import com.peterlaurence.trekme.ui.record.components.widgets.PathData
import com.peterlaurence.trekme.ui.record.components.widgets.StartStopShape
import com.peterlaurence.trekme.ui.record.components.widgets.TwoStateButton
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.GpxRecordServiceViewModel

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

        TwoStateButton(
            Modifier.size(48.dp),
            isDestState = state == GpxRecordState.STOPPED,
            PathData(playPath, Color(0xFF4CAF50)),
            PathData(stopPath, Color(0xFFF44336)),
            onClick = {
                onStartStopClick()
            }
        )
        Spacer(modifier = Modifier.width((30 * animatedValue).dp))

        TwoStateButton(
            Modifier.size(48.dp * animatedValue),
            isDestState = state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED,
            PathData(pausePath, Color(0xFFFFC107)),
            PathData(playPathDest, Color(0xFF4CAF50)),
            showTimeout = false,
            onClick = onPauseResumeClick
        )
    }
}

class PlayPauseStopView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpxRecordServiceViewModel =
            viewModel(findFragment<RecordFragment>().requireActivity())
        val state by viewModel.status.collectAsState()

        TrekMeTheme {
            PlayPauseStop(
                state,
                viewModel::onStartStopClicked,
                viewModel::onPauseResumeClicked
            )
        }
    }
}

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
