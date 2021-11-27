package com.peterlaurence.trekme.ui.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private fun deNormalize(t: Double, min: Double, max: Double): Double {
        return min + t * (max - min)
    }
}