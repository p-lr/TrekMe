package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.m3.TrekMeTheme

@Composable
internal fun PendingScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator()
        Text(
            text = stringResource(id = R.string.loading_maps),
            Modifier.padding(top = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(heightDp = 450)
@Preview(heightDp = 450, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PendingScreenPreview() {
    TrekMeTheme {
        PendingScreen()
    }
}