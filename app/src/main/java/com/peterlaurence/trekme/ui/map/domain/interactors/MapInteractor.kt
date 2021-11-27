package com.peterlaurence.trekme.ui.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.ui.map.domain.models.LandmarkWithNormalizedPos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class MapInteractor @Inject constructor(
    private val mapLoader: MapLoader,
    @ApplicationScope private val scope: CoroutineScope
) {

    /**
     * Update the landmark position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    fun updateAndSaveLandmark(landmark: Landmark, map: Map, x: Double, y: Double)  = scope.launch {
        val mapBounds = map.mapBounds ?: return@launch
        val relativeX = deNormalize(x, mapBounds.X0, mapBounds.X1)
        val relativeY = deNormalize(y, mapBounds.Y0, mapBounds.Y1)

        val lonLat = withContext(Dispatchers.Default) {
            map.projection?.undoProjection(relativeX, relativeY)
        } ?: doubleArrayOf(relativeX, relativeY)

        landmark.apply {
            lat = lonLat[1]
            lon = lonLat[0]
            proj_x = relativeX
            proj_y = relativeY
        }

        mapLoader.saveLandmarks(map)
    }

    /**
     * Given a [Map], get the list of [Landmark] along with their normalized position.
     */
    suspend fun getLandmarkPositions(map: Map) : List<LandmarkWithNormalizedPos> {
        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)

        val landmarks = map.landmarks ?: return emptyList()
        val mapBounds = map.mapBounds ?: return emptyList()

        return landmarks.map { landmark ->
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(landmark.lat, landmark.lon)
            } ?: doubleArrayOf(landmark.lon, landmark.lat)

            val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
            val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)

            LandmarkWithNormalizedPos(landmark, x, y)
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    private fun deNormalize(t: Double, min: Double, max: Double): Double {
        return min + t * (max - min)
    }
}