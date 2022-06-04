package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.OutlinedButtonColored
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.Error

@Composable
fun ErrorScaffold(
    error: Error,
    onMainMenuClick: () -> Unit,
    onShopClick: () -> Unit
) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()

    Scaffold(
        Modifier,
        scaffoldState = scaffoldState,
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
    ) {
        when (error) {
            Error.LicenseError -> MissingOffer(
                message = stringResource(R.string.missing_extended_offer),
                onShopClick
            )
            Error.EmptyMap -> ErrorScreen(message = "empty map")
        }
    }
}

@Composable
private fun MissingOffer(message: String, onShopClick: () -> Unit) {
    Column(
        Modifier
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
            color = textColor(),
            fontSize = 17.sp,
            textAlign = TextAlign.Center
        )
        OutlinedButtonColored(
            onClick = onShopClick,
            text = stringResource(id = R.string.navigate_to_shop),
            color = colorResource(id = R.color.colorGreen)
        )
    }
}