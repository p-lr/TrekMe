@file:OptIn(ExperimentalMaterial3Api::class)

package com.peterlaurence.trekme.features.map.presentation.ui.trackcreate

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.Loading
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.TrackCreateViewModel
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun TrackCreateStateful(
    viewModel: TrackCreateViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    when (uiState) {
        Loading -> LoadingScreen()
        is MapUiState -> {
            TrackCreateScaffold(
                mapState = (uiState as MapUiState).mapState,
                snackbarHostState = snackbarHostState,
                onClose = {
                    onBack()  // TODO : ask confirmation
                }
            )
        }
    }
}

@Composable
private fun TrackCreateScaffold(
    mapState: MapState,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopBar(onCloseClick = onClose)
        }
    ) { paddingValues ->
        TrackCreateScreen(
            Modifier.padding(paddingValues),
            mapState
        )
    }
}

@Composable
private fun TopBar(onCloseClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.track_create_title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_dialog))
            }
        },
    )
}

@Composable
private fun TrackCreateScreen(modifier: Modifier, mapState: MapState) {
    MapUI(
        modifier = modifier,
        state = mapState
    )
}