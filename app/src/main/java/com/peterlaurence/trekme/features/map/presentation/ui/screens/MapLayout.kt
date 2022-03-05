package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.features.map.presentation.ui.components.*
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MapLayout(
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