@file:OptIn(ExperimentalFoundationApi::class)

package com.peterlaurence.trekme.features.map.presentation.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Surface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.features.common.presentation.ui.bottomsheet.States
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_light_background
import com.peterlaurence.trekme.features.map.app.intents.itineraryToMarker
import com.peterlaurence.trekme.features.map.app.service.TrackFollowService
import com.peterlaurence.trekme.features.map.presentation.events.BeaconEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ExcursionWaypointEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ItineraryEvent
import com.peterlaurence.trekme.features.map.presentation.events.MarkerEditEvent
import com.peterlaurence.trekme.features.map.presentation.ui.bottomsheet.BottomSheet
import com.peterlaurence.trekme.features.map.presentation.ui.components.CompassComponent
import com.peterlaurence.trekme.features.map.presentation.ui.components.MapTopAppBar
import com.peterlaurence.trekme.features.map.presentation.ui.components.RecordingButtons
import com.peterlaurence.trekme.features.map.presentation.ui.components.StatsPanel
import com.peterlaurence.trekme.features.map.presentation.ui.components.statsPanelHeight
import com.peterlaurence.trekme.features.map.presentation.ui.screens.ErrorScaffold
import com.peterlaurence.trekme.features.map.presentation.ui.screens.MapScreen
import com.peterlaurence.trekme.features.map.presentation.viewmodel.Error
import com.peterlaurence.trekme.features.map.presentation.viewmodel.GpxRecordServiceViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapEvent
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.StatisticsViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.TrackFollowLayer
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimSolutionDialog
import com.peterlaurence.trekme.features.record.presentation.ui.components.dialogs.BatteryOptimWarningDialog
import com.peterlaurence.trekme.util.android.getActivityOrNull
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.rotation
import java.util.UUID

@Composable
fun MapStateful(
    viewModel: MapViewModel = viewModel(),
    statisticsViewModel: StatisticsViewModel = viewModel(),
    gpxRecordServiceViewModel: GpxRecordServiceViewModel = viewModel(),
    onNavigateToTracksManage: () -> Unit,
    onNavigateToMarkersManage: () -> Unit,
    onNavigateToMarkerEdit: (markerId: String, mapId: UUID) -> Unit,
    onNavigateToExcursionWaypointEdit: (waypointId: String, excursionId: String) -> Unit,
    onNavigateToBeaconEdit: (beaconId: String, mapId: UUID) -> Unit,
    onNavigateToShop: () -> Unit,
    onMainMenuClick: () -> Unit
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
    val isShowingZoomIndicator by viewModel.settings.getShowZoomIndicator()
        .collectAsState(initial = false)
    val recordingStats by statisticsViewModel.stats.collectAsState(initial = null)
    val rotationMode by viewModel.settings.getRotationMode()
        .collectAsState(initial = RotationMode.NONE)

    val lifecycleOwner = LocalLifecycleOwner.current
    val locationFlow = viewModel.locationFlow
    val elevationFix by viewModel.elevationFixFlow.collectAsState()

    val density = LocalDensity.current
    val anchoredDraggableState = remember {
        AnchoredDraggableState(
            initialValue = States.COLLAPSED,
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    LaunchedEffectWithLifecycle {
        viewModel.liveRouteLayer.drawLiveRoute()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val itineraryError = stringResource(id = R.string.itinerary_error)
    val ok = stringResource(id = R.string.ok_dialog)
    LaunchedEffectWithLifecycle(flow = viewModel.placeableEvents) { event ->
        when (event) {
            is MarkerEditEvent -> onNavigateToMarkerEdit(event.marker.id, event.mapId)
            is ExcursionWaypointEditEvent -> onNavigateToExcursionWaypointEdit(
                event.waypoint.id,
                event.excursionId
            )

            is BeaconEditEvent -> onNavigateToBeaconEdit(event.beacon.id, event.mapId)
            is ItineraryEvent -> {
                val success = itineraryToMarker(event.latitude, event.longitude, context)
                if (!success) {
                    showSnackbar(scope, snackbarHostState, itineraryError, ok)
                }
            }
        }
    }
    LaunchedEffect(lifecycleOwner) {
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                locationFlow.collect {
                    viewModel.locationOrientationLayer.onLocation(it)
                    viewModel.excursionWaypointLayer.onLocation(it)
                    viewModel.markerLayer.onLocation(it)
                }
            }
        }
        launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.checkMapLicense()
            }
        }
        launch {
            viewModel.startTrackFollowEvent.collect {
                val intent = Intent(context, TrackFollowService::class.java)
                context.startService(intent)
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

    val outOfBounds = stringResource(id = R.string.map_screen_loc_outside_map)
    var showTrackFollowDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffectWithLifecycle(flow = viewModel.events) { event ->
        fun dismissSnackbar() = scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
        when (event) {
            MapEvent.CURRENT_LOCATION_OUT_OF_BOUNDS -> showSnackbar(
                scope = scope,
                snackbarHostState = snackbarHostState,
                msg = outOfBounds,
                okString = ok
            )

            MapEvent.AWAITING_LOCATION -> {
                val awaitingLocation = context.getString(R.string.awaiting_location)
                showSnackbar(scope, snackbarHostState, awaitingLocation, ok)
            }

            MapEvent.TRACK_TO_FOLLOW_SELECTED -> dismissSnackbar()
            MapEvent.TRACK_TO_FOLLOW_ALREADY_RUNNING -> {
                showTrackFollowDialog = true
            }

            MapEvent.SHOW_TRACK_BOTTOM_SHEET -> {
                scope.launch {
                    anchoredDraggableState.animateTo(States.PEAKED)
                }
            }
        }
    }

    val selectTrack = stringResource(id = R.string.select_track_to_follow)
    var isShowingBatteryWarning by rememberSaveable { mutableStateOf(false) }
    var isShowingBatterySolution by rememberSaveable { mutableStateOf(false) }

    LaunchedEffectWithLifecycle(flow = viewModel.trackFollowLayer.events) { event ->
        when (event) {
            TrackFollowLayer.Event.DisableBatteryOptSignal -> isShowingBatteryWarning = true
            TrackFollowLayer.Event.SelectTrackToFollow -> {
                showSnackbar(scope, snackbarHostState, selectTrack, ok)
            }
        }
    }

    when (uiState) {
        Loading -> {
            LoadingScreen()
        }

        is MapUiState -> {
            val mapUiState = uiState as MapUiState
            val name by mapUiState.mapNameFlow.collectAsStateWithLifecycle()

            BoxWithConstraints {
                val screenHeightDp = maxHeight
                val screenHeightPx = with(LocalDensity.current) {
                    screenHeightDp.toPx()
                }
                val navBarHeightDp =
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val navBarHeightPx = with(LocalDensity.current) {
                    navBarHeightDp.toPx()
                }

                // The height of the banner which appears during a recording
                val recordingBannerHeight = with(LocalDensity.current) {
                    statsPanelHeight.toPx()
                }

                val bottomSheetOffset by remember(screenHeightPx, recordingStats) {
                    derivedStateOf {
                        if (anchoredDraggableState.currentValue == States.COLLAPSED) {
                            if (recordingStats != null) recordingBannerHeight else 0f
                        } else {
                            val offset = anchoredDraggableState.offset
                            if (!offset.isNaN()) {
                                screenHeightPx - offset - navBarHeightPx
                            } else 0f
                        }
                    }
                }

                LaunchedEffect(anchoredDraggableState.currentValue) {
                    val ratio = when (anchoredDraggableState.currentValue) {
                        States.EXPANDED -> expandedRatio
                        States.PEAKED -> peakedRatio
                        States.COLLAPSED -> 0f
                    }
                    viewModel.bottomSheetLayer.onBottomPadding(
                        bottom = screenHeightDp * ratio,
                        withCenter = ratio == expandedRatio
                    )
                }

                /* Always use the light theme background (dark theme or not). Done this way, it
                 * doesn't add a GPU overdraw. */
                TrekMeTheme(darkThemeBackground = md_theme_light_background) {
                    MapScaffold(
                        mapUiState,
                        name,
                        snackbarHostState,
                        isShowingOrientation,
                        isShowingDistance,
                        isShowingDistanceOnTrack,
                        isShowingSpeed,
                        isLockedOnpPosition,
                        isShowingGpsData,
                        isShowingScaleIndicator,
                        isShowingZoomIndicator,
                        rotationMode,
                        locationFlow,
                        elevationFix,
                        geoStatistics = recordingStats,
                        hasElevationFix = purchased,
                        hasBeacons = purchased,
                        hasTrackFollow = purchased,
                        hasMarkerManage = purchased,
                        bottomSheetOffset = bottomSheetOffset,
                        onMainMenuClick = onMainMenuClick,
                        onManageTracks = onNavigateToTracksManage,
                        onManageMarkers = onNavigateToMarkersManage,
                        onToggleShowOrientation = viewModel::toggleShowOrientation,
                        onAddMarker = viewModel.markerLayer::addMarker,
                        onAddLandmark = viewModel.landmarkLayer::addLandmark,
                        onAddBeacon = viewModel.beaconLayer::addBeacon,
                        onShowDistance = viewModel.distanceLayer::toggleDistance,
                        onToggleDistanceOnTrack = viewModel.routeLayer::toggleDistanceOnTrack,
                        onToggleSpeed = viewModel::toggleSpeed,
                        onToggleLockOnPosition = viewModel.locationOrientationLayer::toggleLockedOnPosition,
                        onToggleShowGpsData = viewModel::toggleShowGpsData,
                        onFollowTrack = { viewModel.initiateTrackFollow() },
                        onPositionFabClick = viewModel.locationOrientationLayer::centerOnPosition,
                        onCompassClick = viewModel::alignToNorth,
                        onElevationFixUpdate = viewModel::onElevationFixUpdate,
                        onNavigateToShop = onNavigateToShop,
                        recordingButtons = {
                            RecordingFabStateful(gpxRecordServiceViewModel)
                        }
                    )
                }

                val bottomSheetState by viewModel.bottomSheetLayer.state.collectAsState()
                if (anchoredDraggableState.currentValue != States.COLLAPSED) {
                    BottomSheet(
                        anchoredDraggableState = anchoredDraggableState,
                        screenHeightDp = screenHeightDp,
                        screenHeightPx = screenHeightPx,
                        expandedRatio = expandedRatio,
                        peakedRatio = peakedRatio,
                        bottomSheetState = bottomSheetState,
                        onCursorMove = { latLon, d, ele ->
                            viewModel.calloutLayer.setCursor(latLon, distance = d, ele = ele)
                        },
                        onColorChange = viewModel.bottomSheetLayer::onColorChange,
                        onTitleChange = viewModel.bottomSheetLayer::onTitleChange
                    )
                }
            }
        }

        is Error -> ErrorScaffold(
            uiState as Error,
            onMainMenuClick = onMainMenuClick,
            onShopClick = onNavigateToShop
        )
    }

    if (showTrackFollowDialog) {
        AlertDialog(
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.transit_detour),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(6.dp)
                            .align(Alignment.Center),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        contentDescription = null
                    )
                }
            },
            text = {
                Text(
                    stringResource(id = R.string.track_follow_already_running),
                    fontSize = 16.sp,
                    style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto)
                )
            },
            confirmButton = {
                TextButton(onClick = { showTrackFollowDialog = false }) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        /* Stop the service */
                        val intent = Intent(context, TrackFollowService::class.java)
                        intent.action = TrackFollowService.stopAction
                        context.startService(intent)

                        showTrackFollowDialog = false
                    }
                ) {
                    Text(text = stringResource(id = R.string.service_track_follow_stop))
                }
            },
            onDismissRequest = {
                showTrackFollowDialog = false
            }
        )
    }

    if (isShowingBatteryWarning) {
        BatteryOptimWarningDialog(
            text = stringResource(id = R.string.battery_warn_message_track_follow),
            onShowSolution = {
                isShowingBatterySolution = true
                isShowingBatteryWarning = false
            },
            onDismissRequest = {
                isShowingBatteryWarning = false
                viewModel.trackFollowLayer.ackBatteryOptSignal.trySend(Unit)
            },
        )
    }

    if (isShowingBatterySolution) {
        BatteryOptimSolutionDialog(
            onDismissRequest = {
                isShowingBatterySolution = false
                viewModel.trackFollowLayer.ackBatteryOptSignal.trySend(Unit)
            }
        )
    }
}

@Composable
private fun MapScaffold(
    uiState: MapUiState,
    name: String,
    snackbarHostState: SnackbarHostState,
    isShowingOrientation: Boolean,
    isShowingDistance: Boolean,
    isShowingDistanceOnTrack: Boolean,
    isShowingSpeed: Boolean,
    isLockedOnPosition: Boolean,
    isShowingGpsData: Boolean,
    isShowingScaleIndicator: Boolean,
    isShowingZoomIndicator: Boolean,
    rotationMode: RotationMode,
    locationFlow: Flow<Location>,
    elevationFix: Int,
    geoStatistics: GeoStatistics?,
    hasElevationFix: Boolean,
    hasBeacons: Boolean,
    hasTrackFollow: Boolean,
    hasMarkerManage: Boolean,
    bottomSheetOffset: Float,
    onMainMenuClick: () -> Unit,
    onManageTracks: () -> Unit,
    onManageMarkers: () -> Unit,
    onToggleShowOrientation: () -> Unit,
    onAddMarker: () -> Unit,
    onAddLandmark: () -> Unit,
    onAddBeacon: () -> Unit,
    onShowDistance: () -> Unit,
    onToggleDistanceOnTrack: () -> Unit,
    onToggleSpeed: () -> Unit,
    onToggleLockOnPosition: () -> Unit,
    onToggleShowGpsData: () -> Unit,
    onFollowTrack: () -> Unit,
    onPositionFabClick: () -> Unit,
    onCompassClick: () -> Unit,
    onElevationFixUpdate: (Int) -> Unit,
    onNavigateToShop: () -> Unit,
    recordingButtons: @Composable () -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.graphicsLayer {
                    translationY = -bottomSheetOffset
                },
                hostState = snackbarHostState
            )
        },
        topBar = {
            MapTopAppBar(
                title = name,
                isShowingOrientation = isShowingOrientation,
                isShowingDistance = isShowingDistance,
                isShowingDistanceOnTrack = isShowingDistanceOnTrack,
                isShowingSpeed = isShowingSpeed,
                isLockedOnPosition = isLockedOnPosition,
                isShowingGpsData = isShowingGpsData,
                hasBeacons = hasBeacons,
                hasTrackFollow = hasTrackFollow,
                hasMarkerManage = hasMarkerManage,
                onMenuClick = onMainMenuClick,
                onManageTracks = onManageTracks,
                onManageMarkers = onManageMarkers,
                onToggleShowOrientation = onToggleShowOrientation,
                onAddMarker = onAddMarker,
                onAddLandmark = onAddLandmark,
                onAddBeacon = onAddBeacon,
                onShowDistance = onShowDistance,
                onToggleDistanceOnTrack = onToggleDistanceOnTrack,
                onToggleSpeed = onToggleSpeed,
                onToggleLockPosition = onToggleLockOnPosition,
                onToggleShowGpsData = onToggleShowGpsData,
                onFollowTrack = onFollowTrack,
                onNavigateToShop = onNavigateToShop
            )
        },
        floatingActionButton = {
            Column(
                Modifier
                    .graphicsLayer {
                        translationY = -bottomSheetOffset
                    }
            ) {
                if (rotationMode != RotationMode.NONE) {
                    CompassComponent(
                        degrees = uiState.mapState.rotation,
                        onClick = if (rotationMode == RotationMode.FREE) onCompassClick else null
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
        Column(Modifier.padding(paddingValues)) {
            MapScreen(
                modifier = Modifier.weight(1f, true),
                mapUiState = uiState,
                isShowingDistance = isShowingDistance,
                isShowingSpeed = isShowingSpeed,
                isShowingGpsData = isShowingGpsData,
                isShowingScaleIndicator = isShowingScaleIndicator,
                isShowingZoomIndicator = isShowingZoomIndicator,
                locationFlow = locationFlow,
                elevationFix = elevationFix,
                hasElevationFix = hasElevationFix,
                onElevationFixUpdate = onElevationFixUpdate,
                recordingButtons = recordingButtons
            )
            if (geoStatistics != null) {
                StatsPanel(stats = geoStatistics)
            }
        }
    }
}

@Composable
private fun RecordingFabStateful(viewModel: GpxRecordServiceViewModel) {
    val gpxRecordState by viewModel.status.collectAsState()
    var isShowingBatteryWarning by rememberSaveable { mutableStateOf(false) }
    var isShowingBatterySolution by rememberSaveable { mutableStateOf(false) }

    LaunchedEffectWithLifecycle(flow = viewModel.events) { event ->
        when (event) {
            GpxRecordServiceViewModel.Event.DisableBatteryOptSignal -> {
                isShowingBatteryWarning = true
            }
        }
    }

    if (isShowingBatteryWarning) {
        BatteryOptimWarningDialog(
            text = stringResource(id = R.string.battery_warn_message_gpx_recording),
            onShowSolution = {
                isShowingBatterySolution = true
                isShowingBatteryWarning = false
            },
            onDismissRequest = {
                isShowingBatteryWarning = false
                viewModel.ackBatteryOptSignal.trySend(Unit)
            },
        )
    }

    if (isShowingBatterySolution) {
        BatteryOptimSolutionDialog(
            onDismissRequest = {
                isShowingBatterySolution = false
                viewModel.ackBatteryOptSignal.trySend(Unit)
            }
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
 * and below, we need the activity.
 *
 * @return The angle in decimal degrees
 */
@Composable
private fun getDisplayRotation(): Int {
    val surfaceRotation: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        @Suppress("DEPRECATION")
        LocalContext.current.getActivityOrNull()?.windowManager?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
    } else {
        LocalContext.current.display.rotation
    }

    return when (surfaceRotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

fun showSnackbar(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    msg: String,
    okString: String
) = scope.launch {
    /* Dismiss the currently showing snackbar, if any */
    snackbarHostState.currentSnackbarData?.dismiss()

    snackbarHostState.showSnackbar(msg, actionLabel = okString)
}

private const val expandedRatio = 0.5f
private const val peakedRatio = 0.3f
