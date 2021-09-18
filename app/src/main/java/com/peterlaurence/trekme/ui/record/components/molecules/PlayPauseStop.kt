package com.peterlaurence.trekme.ui.record.components.molecules

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
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
    Row {
        TwoStateButton(
            Modifier.size(48.dp),
            isDestState = state == GpxRecordState.STOPPED,
            PathData(playPath, Color(0xFF4CAF50)),
            PathData(stopPath, Color(0xFFF44336)),
            onClick = onStartStopClick
        )
        when (state) {
            GpxRecordState.STOPPED -> { /* Nothing to do */
            }
            GpxRecordState.STARTED, GpxRecordState.PAUSED -> {
                TwoStateButton(
                    Modifier.size(48.dp),
                    isDestState = state == GpxRecordState.STARTED,
                    PathData(pausePath, Color(0xFFFFC107)),
                    PathData(playPathDest, Color(0xFF4CAF50)),
                    onClick = onPauseResumeClick
                )
            }
        }
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
            PlayPauseStop(state, viewModel::onStartStopClicked, viewModel::onPauseResumeClicked)
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
