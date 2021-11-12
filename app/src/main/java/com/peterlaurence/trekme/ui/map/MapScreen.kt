package com.peterlaurence.trekme.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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


    if (uiState is MapUiState) {
        LaunchedEffect(lifecycleOwner, (uiState as MapUiState).isShowingOrientation) {
            if (!(uiState as MapUiState).isShowingOrientation) return@LaunchedEffect
            launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.orientationFlow.collect {
                        println("xxxx orientation $it")
                    }
                }
            }
        }
    }

    MapScaffold(
        uiState,
        topBarState,
        snackBarEvents,
        onSnackBarShown = viewModel.snackBarController::onSnackBarShown,
        onMainMenuClick = viewModel::onMainMenuClick,
        onToggleShowOrientation = viewModel::toggleShowOrientation
    )
}

@Composable
fun MapScaffold(
    uiState: UiState,
    topBarState: TopBarState,
    snackBarEvents: List<SnackBarEvent>,
    onSnackBarShown: () -> Unit,
    onMainMenuClick: () -> Unit,
    onToggleShowOrientation: () -> Unit
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
            if (uiState is MapUiState) {
                MapTopAppBar(uiState.isShowingOrientation, onMenuClick = onMainMenuClick, onToggleShowOrientation = onToggleShowOrientation)
            } else {
                /* In case of error, we only show the main menu button */
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onMainMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = "")
                        }
                    }
                )
            }
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
