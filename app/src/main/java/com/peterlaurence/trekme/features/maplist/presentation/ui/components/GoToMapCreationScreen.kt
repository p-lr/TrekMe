package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R

@Composable
internal fun GoToMapCreationScreen(onButtonCLick: (showOnBoarding: Boolean) -> Unit) {
    BoxWithConstraints {
        val maxWidth = maxWidth
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.create_first_map_question),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onButtonCLick(true) },
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(text = stringResource(id = R.string.with_onboarding_btn).uppercase())
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onButtonCLick(false) },
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(stringResource(id = R.string.without_onboarding_btn).uppercase())
            }
        }
    }
}