package com.peterlaurence.trekme.viewmodel.map

import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.map.components.LandMark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addMarker
import java.util.*

class LandmarkLayer(
    private val scope: CoroutineScope,
    private val mapLoader: MapLoader,
    mapRepository: MapRepository,
    uiStateFlow: StateFlow<UiState>
) {
    private var imported = false
    private var landmarkListState = listOf<LandmarkState>()

    init {
        mapRepository.mapFlow.combine(
            uiStateFlow.filterIsInstance<MapUiState>()
        ) { map, mapUiState ->
            if (map != null) {
                onMapUpdate(map, mapUiState)
            }
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapUiState: MapUiState) {
        if (imported) return
        val mapBounds = map.mapBounds ?: return

        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)
        val landmarks = map.landmarks ?: return

        landmarkListState = landmarks.map { landmark ->
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(landmark.lat, landmark.lon)
            } ?: doubleArrayOf(landmark.lon, landmark.lat)

            val id = UUID.randomUUID()
            val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
            val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)
            mapUiState.mapState.addMarker(id.toString(), x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                LandMark(isStatic = true)
            }

            LandmarkState(id, landmark)
        }
        imported = true
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private data class LandmarkState(val id: UUID, val landmark: Landmark)