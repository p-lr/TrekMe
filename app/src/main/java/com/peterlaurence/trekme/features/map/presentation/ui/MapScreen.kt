package com.peterlaurence.trekme.features.map.presentation.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.features.map.presentation.ui.components.*
import com.peterlaurence.trekme.features.map.presentation.viewmodel.*
import com.peterlaurence.trekme.ui.gpspro.screens.ErrorScreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.StatisticsViewModel
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.rotation
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onNavigateToTracksManage: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isShowingOrientation by viewModel.orientationVisibilityFlow()
        .collectAsState(initial = false)
    val isShowingDistance by viewModel.isShowingDistanceFlow().collectAsState()
    val isShowingDistanceOnTrack by viewModel.isShowingDistanceOnTrackFlow().collectAsState()
    val isShowingSpeed by viewModel.isShowingSpeedFlow().collectAsState(initial = false)
    val isLockedOnpPosition by viewModel.isLockedOnPosition()
    val isShowingGpsData by viewModel.isShowingGpsDataFlow().collectAsState(initial = false)
    val snackBarEvents = viewModel.snackBarController.snackBarEvents.toList()
    val stats by statisticsViewModel.stats.collectAsState(initial = null)
    val location: Location? by viewModel.locationFlow.collectAsState(initial = null)
    val rotationMode by viewModel.settings.getRotationMode()
        .collectAsState(initial = RotationMode.NONE)

    LaunchedEffect(lifecycleOwner) {
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.locationFlow.collect {
                    viewModel.locationOrientationLayer.onLocation(it)
                }
            }
        }
        launch {
            viewModel.checkMapLicense()
        }
    }

    if (uiState is MapUiState) {
        val displayRotation = getDisplayRotation()
        LaunchedEffect(lifecycleOwner, isShowingOrientation) {
            if (!isShowingOrientation) return@LaunchedEffect
            launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    viewModel.orientationFlow.collect {
                        viewModel.locationOrientationLayer.onOrientation(it, displayRotation)
                    }
                }
            }
        }
    }

    when (uiState) {
        Loading -> Text(text = "loading")  // TODO: improve that
        is MapUiState -> {
            Column {
                MapScaffold(
                    Modifier.weight(1f, true),
                    uiState as MapUiState,
                    isShowingOrientation,
                    isShowingDistance,
                    isShowingDistanceOnTrack,
                    isShowingSpeed,
                    isLockedOnpPosition,
                    isShowingGpsData,
                    rotationMode,
                    snackBarEvents,
                    location,
                    onSnackBarShown = viewModel.snackBarController::onSnackBarShown,
                    onMainMenuClick = viewModel::onMainMenuClick,
                    onManageTracks = onNavigateToTracksManage,
                    onToggleShowOrientation = viewModel::toggleShowOrientation,
                    onAddMarker = viewModel.markerLayer::addMarker,
                    onAddLandmark = viewModel.landmarkLayer::addLandmark,
                    onShowDistance = viewModel.distanceLayer::toggleDistance,
                    onToggleDistanceOnTrack = viewModel.routeLayer::toggleDistanceOnTrack,
                    onToggleSpeed = viewModel::toggleSpeed,
                    onToggleLockOnPosition = viewModel.locationOrientationLayer::toggleLockedOnPosition,
                    onToggleShowGpsData = viewModel::toggleShowGpsData,
                    onPositionFabClick = viewModel.locationOrientationLayer::centerOnPosition,
                    onCompassClick = viewModel::alignToNorth
                )

                stats?.also {
                    StatsPanel(it)
                }
            }
        }
        is Error -> ErrorScaffold(
            uiState as Error,
            onMainMenuClick = viewModel::onMainMenuClick
        )
    }
}

@Composable
fun ErrorScaffold(
    error: Error,
    onMainMenuClick: () -> Unit
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
            Error.LicenseError -> ErrorScreen(message = stringResource(R.string.missing_ign_license))
            Error.EmptyMap -> ErrorScreen(message = "empty map")
        }
    }
}

@Composable
fun MapScaffold(
    modifier: Modifier = Modifier,
    uiState: MapUiState,
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    rotationMode: RotationMode,
    snackBarEvents: List<SnackBarEvent>,
    location: Location?,
    onSnackBarShown: () -> Unit,
    onMainMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockOnPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit,
    onPositionFabClick: () -> Unit,
    onCompassClick: () -> Unit
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
        modifier,
        scaffoldState = scaffoldState,
        topBar = {
            MapTopAppBar(
                isShowingOrientation,
                isShowingDistance,
                isShowingDistanceOnTrack,
                isShowingSpeed,
                isLockedOnPosition,
                isShowingGpsData,
                onMenuClick = onMainMenuClick,
                onManageTracks = onManageTracks,
                onToggleShowOrientation = onToggleShowOrientation,
                onAddMarker = onAddMarker,
                onAddLandmark = onAddLandmark,
                onShowDistance = onShowDistance,
                onToggleDistanceOnTrack = onToggleDistanceOnTrack,
                onToggleSpeed = onToggleSpeed,
                onToggleLockPosition = onToggleLockOnPosition,
                onToggleShowGpsData = onToggleShowGpsData
            )
        },
        floatingActionButton = {
            Column {
                if (rotationMode != RotationMode.NONE) {
                    CompassFab(
                        degrees = uiState.mapState.rotation,
                        onClick = if (rotationMode == RotationMode.FREE) onCompassClick else {
                            {}
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                FloatingActionButton(
                    backgroundColor = Color.White,
                    onClick = onPositionFabClick,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                        contentDescription = stringResource(id = R.string.center_on_position_btn_desc),
                        colorFilter = ColorFilter.tint(colorResource(id = R.color.colorAccent))
                    )
                }
            }
        }
    ) {
        MapLayout(
            uiState,
            isShowingDistance,
            isShowingSpeed,
            isShowingGpsData,
            true, // TODO: use settings
            location,
        )
    }
}

@Composable
private fun MapLayout(
    mapUiState: MapUiState,
    isShowingDistance: Boolean,
    isShowingSpeed: Boolean,
    isShowingGpsData: Boolean,
    isShowingScaleIndicator: Boolean,
    location: Location?,
) {
    Box {
        MapUI(state = mapUiState.mapState) {
            val landmarkPositions = mapUiState.landmarkLinesState.landmarksSnapshot
            if (landmarkPositions.isNotEmpty()) {
                LandmarkLines(
                    mapState = mapUiState.mapState,
                    positionMarker = mapUiState.landmarkLinesState.positionMarkerSnapshot,
                    landmarkPositions = landmarkPositions,
                    distanceForIdFlow = mapUiState.landmarkLinesState.distanceForLandmark
                )
            }

            if (isShowingDistance) {
                DistanceLine(
                    mapState = mapUiState.mapState,
                    marker1 = mapUiState.distanceLineState.marker1Snapshot,
                    marker2 = mapUiState.distanceLineState.marker2Snapshot
                )
            }
        }

        Column {
            if (isShowingDistance || isShowingSpeed) {
                val distance by mapUiState.distanceLineState.distanceFlow.collectAsState(initial = 0f)
                TopOverlay(
                    speed = location?.speed,
                    distance = distance,
                    speedVisibility = isShowingSpeed,
                    distanceVisibility = isShowingDistance
                )
            }
            if (isShowingScaleIndicator) {
                val state = mapUiState.scaleIndicatorState
                ScaleIndicator(
                    widthPx = state.widthPx,
                    widthRatio = state.widthRatio,
                    scaleText = state.scaleText
                )
            }
        }

        if (isShowingGpsData) {
            GpsDataOverlay(location, Modifier.align(Alignment.BottomStart))
        }
    }
}

/**
 * We need to know the display rotation (either 0, 90°, 180°, or 270°) - and not just the
 * portrait / landscape mode.
 * To get that information, we only need a [Context] for Android 11 and up. However, on Android 10
 * and below, we need the [AppCompatActivity].
 *
 * @return The angle in decimal degrees
 */
@Composable
private fun getDisplayRotation(): Int {
    val surfaceRotation: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        LocalContext.current.getActivity()?.windowManager?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
    } else {
        LocalContext.current.display?.rotation ?: Surface.ROTATION_0
    }

    return when (surfaceRotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

/**
 * Depending on where the compose tree was originally created, we might have a [ContextWrapper].
 */
private tailrec fun Context.getActivity(): AppCompatActivity? = when (this) {
    is AppCompatActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
