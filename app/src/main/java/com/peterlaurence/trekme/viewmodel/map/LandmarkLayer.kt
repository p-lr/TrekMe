package com.peterlaurence.trekme.viewmodel.map

import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.map.components.LandMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addMarker
import java.util.*

class LandmarkLayer(
    private val scope: CoroutineScope,
    private val mapLoader: MapLoader,
    uiStateFlow: StateFlow<UiState>
) {
    private var landmarkListState = listOf<LandmarkState>()

    init {
        uiStateFlow.filterIsInstance<MapUiState>().map { mapUiState ->
            onMapUpdate(mapUiState)
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(mapUiState: MapUiState) {
        val map = mapUiState.map
        val mapBounds = map.mapBounds ?: return

        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)
        val landmarks = map.landmarks ?: return

        landmarkListState = landmarks.map { landmark ->
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(landmark.lat, landmark.lon)
            } ?: doubleArrayOf(landmark.lon, landmark.lat)

            val id = UUID.randomUUID().toString()
            val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
            val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)

            mapUiState.mapState.addMarker(id, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                LandMark(isStatic = true)
            }

            LandmarkState(id, landmark)
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private data class LandmarkState(val id: String, val landmark: Landmark)