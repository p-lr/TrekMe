package com.peterlaurence.trekme.features.record.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.HeartBeatIndicator
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.features.record.presentation.viewmodel.GpxRecordServiceViewModel

@Composable
fun StatusStateful(modifier: Modifier = Modifier, viewModel: GpxRecordServiceViewModel) {
    val gpxRecordState by viewModel.status.collectAsState()

    when (gpxRecordState) {
        GpxRecordState.STOPPED -> Status(
            modifier = modifier,
            isBeating = false,
            subTitle = stringResource(id = R.string.recording_status_stopped)
        )

        GpxRecordState.STARTED, GpxRecordState.RESUMED -> Status(
            modifier = modifier,
            isBeating = true,
            subTitle = stringResource(id = R.string.recording_status_started)
        )

        GpxRecordState.PAUSED -> Status(
            modifier = modifier,
            isBeating = false,
            subTitle = stringResource(id = R.string.recording_status_paused)
        )
    }
}

@Composable
private fun Status(modifier: Modifier = Modifier, isBeating: Boolean, subTitle: String) {
    ElevatedCard(modifier) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(id = R.string.recordings_status_title),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(subTitle, fontSize = 11.sp)
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



