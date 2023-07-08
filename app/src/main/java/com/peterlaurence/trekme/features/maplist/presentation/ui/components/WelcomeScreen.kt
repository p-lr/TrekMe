package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme

@Composable
internal fun WelcomeScreen(
    onGoToMapCreation: (showOnBoarding: Boolean) -> Unit,
    onGoToExcursionSearch: () -> Unit
) {
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
                onClick = { onGoToMapCreation(true) },
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(text = stringResource(id = R.string.with_onboarding_btn).uppercase())
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onGoToMapCreation(false) },
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(stringResource(id = R.string.without_onboarding_btn).uppercase())
            }

            Box(
                Modifier.padding(vertical = 32.dp)
            ) {
                Divider(Modifier.fillMaxWidth().align(Alignment.Center))
                Text(
                    text = stringResource(id = R.string.welcome_screen_or),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = DividerDefaults.color,
                            shape = RoundedCornerShape(50)
                        )
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp).align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onGoToExcursionSearch,
                modifier = Modifier.width(maxWidth * 0.6f),
            ) {
                Text(text = stringResource(id = R.string.welcome_excursion_search).uppercase())
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    TrekMeTheme {
        WelcomeScreen(onGoToMapCreation = {}, onGoToExcursionSearch = {})
    }
}