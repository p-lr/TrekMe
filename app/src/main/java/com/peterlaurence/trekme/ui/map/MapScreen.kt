package com.peterlaurence.trekme.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.viewmodel.map.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    LaunchedEffect(lifecycleOwner) {
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.locationFlow.collect {
                    viewModel.onLocationReceived(it)
                }
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val snackBarEvents = viewModel.snackBarController.snackBarEvents.toList()

    MapScaffold(
        uiState,
        topBarState,
        snackBarEvents,
        onSnackBarShown = viewModel.snackBarController::onSnackBarShown,
        onMainMenuClick = viewModel::onMainMenuClick
    )
}

@Composable
fun MapScaffold(
    uiState: UiState,
    topBarState: TopBarState,
    snackBarEvents: List<SnackBarEvent>,
    onSnackBarShown: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val scaffoldState: ScaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    if (snackBarEvents.isNotEmpty()) {
        val ok = stringResource(id = R.string.ok_dialog)
        val message = when (snackBarEvents.first()) {
            SnackBarEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> stringResource(id = R.string.map_screen_loc_outside_map)
        }

        SideEffect {
            scope.launch {
                /* Dismiss the currently showing snackbar, if any */
                scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()

                scaffoldState.snackbarHostState
                    .showSnackbar(message, actionLabel = ok)
            }
            onSnackBarShown()
        }
    }

    Scaffold(
        Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        topBar = {
            MapTopAppBar(topBarState, onMenuClick = onMainMenuClick, onShowOrientation = {})
        },
        floatingActionButton = {

        }

    ) {
        when (uiState) {
            Error.LicenseError -> Text(text = "license error")
            Error.EmptyMap -> Text(text = "empty map")
            Loading -> Text(text = "loading")
            is MapUiState -> MapUi(uiState)
        }
    }
}

@Composable
fun MapUi(mapUiState: MapUiState) {
    MapUI(state = mapUiState.mapState)
}
