package com.peterlaurence.trekme.features.map.domain.interactors

import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.core.map.domain.models.Landmark
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.LandmarkWithNormalizedPos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class LandmarkInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val landmarksDao: LandmarksDao,
    @ApplicationScope private val scope: CoroutineScope,
    @ApplicationContext private val context: Context
) {
    /**
     * Create and add a new landmark.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun makeLandmark(map: Map, x: Double, y: Double): Landmark {
        return withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)
            val name = context.getString(R.string.landmark_default_name)

            Landmark(name = name, lat = lonLat[1], lon = lonLat[0])
        }
    }

    /**
     * Update the landmark position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    fun updateAndSaveLandmark(landmark: Landmark, map: Map, x: Double, y: Double) = scope.launch {
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        val updatedLandmark = landmark.copy(lat = lonLat[1], lon = lonLat[0])
        map.landmarks.update {
            it.filterNot { m -> m.id == landmark.id } + updatedLandmark
        }

        landmarksDao.saveLandmarks(map)
    }

    fun deleteLandmark(landmark: Landmark, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        map.landmarks.update { it - landmark }
        landmarksDao.saveLandmarks(map)
    }

    suspend fun getLandmarksFlow(map: Map): Flow<List<LandmarkWithNormalizedPos>> {
        /* Import landmarks */
        landmarksDao.getLandmarksForMap(map)

        return map.landmarks.map { landmarkList ->
            landmarkList.map { landmark ->
                val (x, y) = getNormalizedCoordinates(
                    landmark.lat,
                    landmark.lon,
                    map.mapBounds,
                    map.projection
                )

                LandmarkWithNormalizedPos(landmark, x, y)
            }
        }
    }
}