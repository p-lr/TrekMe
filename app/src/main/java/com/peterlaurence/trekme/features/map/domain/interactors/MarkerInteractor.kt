package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.*
import javax.inject.Inject

class MarkerInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val markersDao: MarkersDao,
    @ApplicationScope private val scope: CoroutineScope
) {
    suspend fun getMarkersFlow(map: Map): Flow<List<MarkerWithNormalizedPos>> {
        /* Import markers */
        markersDao.getMarkersForMap(map)

        return map.markers.map { markerList ->
            markerList.map { marker ->
                val (x, y) = getNormalizedCoordinates(
                    marker.lat,
                    marker.lon,
                    map.mapBounds,
                    map.projection
                )

                MarkerWithNormalizedPos(marker, x, y)
            }
        }
    }

    /**
     * Create a new marker.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun makeMarker(map: Map, x: Double, y: Double): Marker {
        return withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)

            Marker(lat = lonLat[1], lon = lonLat[0])
        }
    }

    /**
     * Update the marker position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    fun updateMarkerPosition(marker: Marker, map: Map, x: Double, y: Double) = scope.launch {
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        val updatedMarker = marker.copy(lat = lonLat[1], lon = lonLat[0])
        map.markers.update {
            it.filterNot { m -> m.id == marker.id } + updatedMarker
        }

        markersDao.saveMarkers(map)
    }

    /**
     * Update markers now.
     */
    fun updateMarkers(markers: List<Marker>, map: Map) = scope.launch {
        val markersById = markers.associateBy { it.id }

        map.markers.update {
            it.map { m ->
                markersById[m.id] ?: m
            }
        }

        markersDao.saveMarkers(map)
    }

    /**
     * Save marker (debounced)
     */
    fun saveMarkerDebounced(mapId: UUID, marker: Marker) {
        updateMarkerJob?.cancel()
        updateMarkerJob = scope.launch {
            delay(1000)
            val map = mapRepository.getMap(mapId) ?: return@launch

            map.markers.update { formerList ->
                formerList.filter { it.id != marker.id } + marker
            }

            markersDao.saveMarkers(map)
        }
    }

    fun deleteMarker(marker: Marker, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        map.markers.update { it - marker }
        markersDao.saveMarkers(map)
    }

    private var updateMarkerJob: Job? = null
}