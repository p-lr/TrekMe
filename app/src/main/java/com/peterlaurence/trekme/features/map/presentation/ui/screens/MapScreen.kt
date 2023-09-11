package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.features.map.presentation.ui.components.DistanceLine
import com.peterlaurence.trekme.features.map.presentation.ui.components.ElevationFixDialog
import com.peterlaurence.trekme.features.map.presentation.ui.components.GpsDataOverlay
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandmarkLines
import com.peterlaurence.trekme.features.map.presentation.ui.components.ScaleIndicator
import com.peterlaurence.trekme.features.map.presentation.ui.components.TopOverlay
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.DistanceLineState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.ScaleIndicatorState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI
import kotlin.time.TimeSource

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapUiState: MapUiState,
    isShowingDistance: Boolean,
    isShowingSpeed: Boolean,
    isShowingGpsData: Boolean,
    isShowingScaleIndicator: Boolean,
    locationState: State<Location?>,
    elevationFix: Int,
    hasElevationFix: Boolean,
    onElevationFixUpdate: (Int) -> Unit,
    recordingButtons: @Composable () -> Unit
) {
    var isShowingElevationFixDialog by remember { mutableStateOf(false) }

    Box(modifier) {
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
                TopOverlay(
                    distanceLineStateProvider = { mapUiState.distanceLineState },
                    locationState = locationState,
                    isShowingSpeed = isShowingSpeed,
                    isShowingDistance = isShowingDistance
                )
            }
            if (isShowingScaleIndicator) {
                ScaleIndicator(
                    scaleIndicatorStateProvider = { mapUiState.scaleIndicatorState }
                )
            }

            recordingButtons()
        }

        if (isShowingGpsData) {
            GpsDataOverlay(
                Modifier.align(Alignment.BottomStart),
                locationState,
                hasElevationFix,
                elevationFix = if (hasElevationFix) elevationFix else 0,
                isComputingElapsedTime = hasElevationFix,
                onFixElevationClick = {
                    isShowingElevationFixDialog = true
                }
            )
        }

        if (isShowingElevationFixDialog) {
            ElevationFixDialog(
                elevationFix,
                onElevationFixUpdate = onElevationFixUpdate,
                onDismiss = {
                    isShowingElevationFixDialog = false
                }
            )
        }
    }
}

/**
 * Defers read of [ScaleIndicatorState].
 */
@Composable
private fun ScaleIndicator(
    scaleIndicatorStateProvider: () -> ScaleIndicatorState,
    color: Color = MaterialTheme.colorScheme.tertiaryContainer
) {
    val state = scaleIndicatorStateProvider()

    ScaleIndicator(
        widthPx = state.widthPx,
        widthRatio = state.widthRatio,
        scaleText = state.scaleText,
        color = color
    )
}

/**
 * Defers read of [DistanceLineState].
 */
@Composable
private fun TopOverlay(
    distanceLineStateProvider: () -> DistanceLineState,
    locationState: State<Location?>,
    isShowingDistance: Boolean,
    isShowingSpeed: Boolean,
) {
    val state = distanceLineStateProvider()
    val distance by state.distanceFlow.collectAsState(initial = 0f)
    TopOverlay(
        speed = locationState.value?.speed,
        distance = distance,
        speedVisibility = isShowingSpeed,
        distanceVisibility = isShowingDistance
    )
}

/**
 * Defers read of location, and compute the elapsed time between the last location update.
 */
@Composable
private fun GpsDataOverlay(
    modifier: Modifier = Modifier,
    locationState: State<Location?>,
    isElevationModifiable: Boolean,
    elevationFix: Int,
    isComputingElapsedTime: Boolean,
    onFixElevationClick: () -> Unit = {}
) {
    val location = locationState.value
    var lastUpdateInSeconds: Long? by remember { mutableStateOf(null) }

    if (isComputingElapsedTime) {
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(key1 = location, key2 = lifecycleOwner) {
            val timeSource = TimeSource.Monotonic
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    while (true) {
                        if (location != null) {
                            lastUpdateInSeconds = (timeSource.markNow() - location.markedTime).inWholeSeconds
                        }
                        delay(1000)
                    }
                }
            }
        }
    }

    GpsDataOverlay(
        modifier,
        location,
        isElevationModifiable,
        elevationFix,
        lastUpdateInSeconds,
        onFixElevationClick
    )
}