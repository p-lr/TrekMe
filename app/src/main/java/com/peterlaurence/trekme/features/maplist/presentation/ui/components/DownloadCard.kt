package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.m3.TrekMeTheme

@Composable
fun DownloadCard(modifier: Modifier = Modifier, downloadProgress: Int, onCancel: () -> Unit) {
    Card(modifier, elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(start = 8.dp, end = 16.dp)) {
            Text(
                stringResource(id = R.string.download_pending_item),
                Modifier.padding(start = 8.dp, top = 8.dp)
            )
            LinearProgressIndicator(
                progress = downloadProgress / 100f,
                modifier = Modifier
                    .padding(start = 9.dp, top = 8.dp, bottom = 4.dp)
                    .fillMaxWidth()
            )

            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(id = R.string.cancel_dialog_string).uppercase(),
                )
            }
        }

    }
}

@Preview(widthDp = 350)
@Composable
private fun DownloadCardPreview() {
    TrekMeTheme {
        DownloadCard(downloadProgress = 25, onCancel = {})
    }
}