package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.OutlinedButtonColored
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.dark_accentGreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.light_accentGreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScaffold(
    error: Error,
    onMainMenuClick: () -> Unit,
    onShopClick: () -> Unit
) {

    Scaffold(
        Modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onMainMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (error) {
            Error.LicenseError -> MissingOffer(
                Modifier.padding(paddingValues),
                message = stringResource(R.string.missing_extended_offer),
                onShopClick
            )
            Error.EmptyMap -> ErrorScreen(
                Modifier.padding(paddingValues),
                message = stringResource(id = R.string.empty_map)
            )
        }
    }
}

@Composable
private fun MissingOffer(modifier: Modifier = Modifier, message: String, onShopClick: () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_emoji_disappointed_face_1f61e),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
        )
        Text(
            text = message,
            modifier = Modifier.padding(vertical = 32.dp),
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        OutlinedButtonColored(
            onClick = onShopClick,
            text = stringResource(id = R.string.navigate_to_shop),
            color = if (isSystemInDarkTheme()) dark_accentGreen else light_accentGreen
        )
    }
}

@Preview(widthDp = 350, heightDp = 400)
@Composable
private fun ErrorPreview1() {
    TrekMeTheme {
        ErrorScaffold(error = Error.EmptyMap, onMainMenuClick = {}, onShopClick = {})
    }
}

@Preview(widthDp = 350, heightDp = 400)
@Composable
private fun ErrorPreview2() {
    TrekMeTheme {
        ErrorScaffold(error = Error.LicenseError, onMainMenuClick = {}, onShopClick = {})
    }
}