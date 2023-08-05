package com.peterlaurence.trekme.features.map.presentation.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_light_background
import com.peterlaurence.trekme.features.map.presentation.ui.components.*
import com.peterlaurence.trekme.features.map.presentation.ui.screens.ErrorScaffold
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MapScreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.*
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimSolutionDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimWarningDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.LocationRationale
import com.peterlaurence.trekme.features.map.presentation.viewmodel.GpxRecordServiceViewModel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.rotation
import java.util.*

@Composable
fun MapStateful(
    viewModel: MapViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel(),
    gpxRecordServiceViewModel: GpxRecordServiceViewModel = viewModel(),
    onNavigateToTracksManage: () -> Unit,
    onNavigateToMarkerEdit: (markerId: String, mapId: UUID) -> Unit,
    onNavigateToBeaconEdit: (beaconId: String, mapId: UUID) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val purchased by viewModel.purchaseFlow.collectAsState()
    val isShowingOrientation by viewModel.orientationVisibilityFlow()
        .collectAsState(initial = false)
    val isShowingDistance by viewModel.isShowingDistanceFlow().collectAsState()
    val isShowingDistanceOnTrack by viewModel.isShowingDistanceOnTrackFlow().collectAsState()
    val isShowingSpeed by viewModel.isShowingSpeedFlow().collectAsState(initial = false)
    val isLockedOnpPosition by viewModel.isLockedOnPosition()
    val isShowingGpsData by viewModel.isShowingGpsDataFlow().collectAsState(initial = false)
    val isShowingScaleIndicator by viewModel.settings.getShowScaleIndicator()
        .collectAsState(initial = true)
    val snackBarEvents = viewModel.snackBarController.snackBarEvents.toList()
    val stats by statisticsViewModel.stats.collectAsState(initial = null)
    val rotationMode by viewModel.settings.getRotationMode()
        .collectAsState(initial = RotationMode.NONE)

    val lifecycleOwner = LocalLifecycleOwner.current
    val locationFlow = viewModel.locationFlow
    val elevationFix by viewModel.elevationFixFlow.collectAsState()

    val locationState: State<Location?> = locationFlow.collectAsStateWithLifecycle(initialValue = null, minActiveState = Lifecycle.State.RESUMED)

    LaunchedEffect(lifecycleOwner) {
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationFlow.collect {
                    viewModel.locationOrientationLayer.onLocation(it)
                }
            }
        }
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.checkMapLicense()
            }
        }
        launch {
            viewModel.markerEditEvent.collect {
                onNavigateToMarkerEdit(it.marker.id, it.mapId)
            }
        }
        launch {
            viewModel.beaconEditEvent.collect {
                onNavigateToBeaconEdit(it.beacon.id, it.mapId)
            }
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
        Loading -> {
            LoadingScreen()
        }
        is MapUiState -> {
            /* Always use the light theme background (dark theme or not). Done this way, it
             * doesn't add a GPU overdraw. */
            TrekMeTheme(darkThemeBackground = md_theme_light_background) {
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
                        isShowingScaleIndicator,
                        rotationMode,
                        snackBarEvents,
                        locationState,
                        elevationFix,
                        hasElevationFix = purchased,
                        hasBeacons = purchased,
                        onSnackBarShown = viewModel.snackBarController::onSnackBarShown,
                        onMainMenuClick = viewModel::onMainMenuClick,
                        onManageTracks = onNavigateToTracksManage,
                        onToggleShowOrientation = viewModel::toggleShowOrientation,
                        onAddMarker = viewModel.markerLayer::addMarker,
                        onAddLandmark = viewModel.landmarkLayer::addLandmark,
                        onAddBeacon = viewModel.beaconLayer::addBeacon,
                        onShowDistance = viewModel.distanceLayer::toggleDistance,
                        onToggleDistanceOnTrack = viewModel.routeLayer::toggleDistanceOnTrack,
                        onToggleSpeed = viewModel::toggleSpeed,
                        onToggleLockOnPosition = viewModel.locationOrientationLayer::toggleLockedOnPosition,
                        onToggleShowGpsData = viewModel::toggleShowGpsData,
                        onPositionFabClick = viewModel.locationOrientationLayer::centerOnPosition,
                        onCompassClick = viewModel::alignToNorth,
                        onElevationFixUpdate = viewModel::onElevationFixUpdate,
                        recordingButtons = {
                            RecordingFabStateful(gpxRecordServiceViewModel)
                        }
                    )

                    stats?.also {
                        StatsPanel(it)
                    }
                }
            }
        }
        is Error -> ErrorScaffold(
            uiState as Error,
            onMainMenuClick = viewModel::onMainMenuClick,
            onShopClick = viewModel::onShopClick
        )
    }
}

@Composable
private fun MapScaffold(
    modifier: Modifier = Modifier,
    uiState: MapUiState,
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    isShowingScaleIndicator: Boolean,
    rotationMode: RotationMode,
    snackBarEvents: List<SnackBarEvent>,
    locationState: State<Location?>,
    elevationFix: Int,
    hasElevationFix: Boolean,
    hasBeacons: Boolean,
    onSnackBarShown: () -> Unit,
    onMainMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onAddBeacon: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockOnPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit,
    onPositionFabClick: () -> Unit,
    onCompassClick: () -> Unit,
    onElevationFixUpdate: (Int) -> Unit,
    recordingButtons: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (snackBarEvents.isNotEmpty()) {
        val ok = stringResource(id = R.string.ok_dialog)
        val message = when (snackBarEvents.first()) {
            SnackBarEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> stringResource(id = R.string.map_screen_loc_outside_map)
        }

        SideEffect {
            scope.launch {
                /* Dismiss the currently showing snackbar, if any */
                snackbarHostState.currentSnackbarData?.dismiss()

                snackbarHostState.showSnackbar(message, actionLabel = ok)
            }
            onSnackBarShown()
        }
    }

    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MapTopAppBar(
                title = uiState.mapName,
                isShowingOrientation = isShowingOrientation,
                isShowingDistance = isShowingDistance,
                isShowingDistanceOnTrack = isShowingDistanceOnTrack,
                isShowingSpeed = isShowingSpeed,
                isLockedOnPosition = isLockedOnPosition,
                isShowingGpsData = isShowingGpsData,
                hasBeacons = hasBeacons,
                onMenuClick = onMainMenuClick,
                onManageTracks = onManageTracks,
                onToggleShowOrientation = onToggleShowOrientation,
                onAddMarker = onAddMarker,
                onAddLandmark = onAddLandmark,
                onAddBeacon = onAddBeacon,
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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = onPositionFabClick,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                        contentDescription = stringResource(id = R.string.center_on_position_btn_desc),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
        }
    ) { paddingValues ->
        MapScreen(
            Modifier.padding(paddingValues),
            uiState,
            isShowingDistance,
            isShowingSpeed,
            isShowingGpsData,
            isShowingScaleIndicator,
            locationState,
            elevationFix,
            hasElevationFix,
            onElevationFixUpdate,
            recordingButtons = recordingButtons
        )
    }
}

@Composable
private fun RecordingFabStateful(viewModel: GpxRecordServiceViewModel) {
    val gpxRecordState by viewModel.status.collectAsState()
    val disableBatterySignal = remember { viewModel.disableBatteryOptSignal.receiveAsFlow() }
    val showLocalisationRationale = remember { viewModel.showLocalisationRationale.receiveAsFlow() }
    var isShowingBatteryWarning by rememberSaveable { mutableStateOf(false) }
    var isShowingBatterySolution by rememberSaveable { mutableStateOf(false) }
    var isShowingLocationRationale by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(LocalLifecycleOwner.current) {
        launch {
            disableBatterySignal.collect {
                isShowingBatteryWarning = true
            }
        }

        launch {
            showLocalisationRationale.collect {
                isShowingLocationRationale = true
            }
        }
    }

    if (isShowingBatteryWarning) {
        BatteryOptimWarningDialog(
            onShowSolution = {
                isShowingBatterySolution = true
                isShowingBatteryWarning = false
            },
            onDismissRequest = { isShowingBatteryWarning = false },
        )
    }

    if (isShowingBatterySolution) {
        BatteryOptimSolutionDialog(onDismissRequest = { isShowingBatterySolution = false })
    }

    if (isShowingLocationRationale) {
        LocationRationale(
            onConfirm = {
                viewModel.requestBackgroundLocationPerm()
                isShowingLocationRationale = false
            },
            onIgnore = {
                viewModel.onIgnoreLocationRationale()
                isShowingLocationRationale = false
            },
        )
    }

    RecordingButtons(
        gpxRecordState,
        onStartStopClick = viewModel::onStartStopClicked,
        onPauseResumeClick = viewModel::onPauseResumeClicked
    )
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
