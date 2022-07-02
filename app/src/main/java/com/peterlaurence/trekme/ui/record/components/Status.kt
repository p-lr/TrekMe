package com.peterlaurence.trekme.ui.record.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.HeartBeatIndicator
import com.peterlaurence.trekme.service.GpxRecordState
import com.peterlaurence.trekme.viewmodel.GpxRecordServiceViewModel

@Composable
fun StatusStateful(viewModel: GpxRecordServiceViewModel) {
    val gpxRecordState by viewModel.status.collectAsState()

    when (gpxRecordState) {
        GpxRecordState.STOPPED -> Status(
            isBeating = false,
            subTitle = stringResource(id = R.string.recording_status_stopped)
        )

        GpxRecordState.STARTED, GpxRecordState.RESUMED -> Status(
            isBeating = true,
            subTitle = stringResource(id = R.string.recording_status_started)
        )

        GpxRecordState.PAUSED -> Status(
            isBeating = false,
            subTitle = stringResource(id = R.string.recording_status_paused)
        )
    }
}

@Composable
private fun Status(modifier: Modifier = Modifier, isBeating: Boolean, subTitle: String) {
    Card(modifier) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(id = R.string.recordings_status_title),
                color = textColor(),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(subTitle, modifier = Modifier.alpha(0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                HeartBeatIndicator(isBeating, outerRadius = 48.dp)
            }
        }
    }
}

@Preview
@Composable
private fun StatusPreview() {
    TrekMeTheme {
        Status(Modifier.size(180.dp, 130.dp), isBeating = true, subTitle = "Started")
    }
}



