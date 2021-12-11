package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapBounds
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.models.LandmarkWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
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
    fun updateAndSaveLandmark(landmark: Landmark, map: Map, x: Double, y: Double) = scope.launch {
        val mapBounds = map.mapBounds ?: return@launch

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        landmark.apply {
            lat = lonLat[1]
            lon = lonLat[0]
        }

        mapLoader.saveLandmarks(map)
    }

    /**
     * Update the marker position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    fun updateAndSaveMarker(marker: Marker, map: Map, x: Double, y: Double) = scope.launch {
        val mapBounds = map.mapBounds ?: return@launch

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        marker.apply {
            lat = lonLat[1]
            lon = lonLat[0]
        }

        mapLoader.saveMarkers(map)
    }

    /**
     * Given a [Map], get the list of [Landmark] along with their normalized position.
     */
    suspend fun getLandmarkPositions(map: Map): List<LandmarkWithNormalizedPos> {
        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)

        val landmarks = map.landmarks ?: return emptyList()
        val mapBounds = map.mapBounds ?: return emptyList()

        return landmarks.map { landmark ->
            val (x, y) = getNormalizedCoordinates(
                landmark.lat,
                landmark.lon,
                mapBounds,
                map.projection
            )

            LandmarkWithNormalizedPos(landmark, x, y)
        }
    }

    suspend fun getMarkerPositions(map: Map): List<MarkerWithNormalizedPos> {
        /* Import markers */
        mapLoader.getMarkersForMap(map)

        val markers = map.markers ?: return emptyList()
        val mapBounds = map.mapBounds ?: return emptyList()

        return markers.map { marker ->
            val (x, y) = getNormalizedCoordinates(
                marker.lat,
                marker.lon,
                mapBounds,
                map.projection
            )

            MarkerWithNormalizedPos(marker, x, y)
        }
    }

    private suspend fun getLonLatFromNormalizedCoordinate(
        x: Double,
        y: Double,
        projection: Projection?,
        mapBounds: MapBounds
    ): DoubleArray {
        val relativeX = deNormalize(x, mapBounds.X0, mapBounds.X1)
        val relativeY = deNormalize(y, mapBounds.Y0, mapBounds.Y1)

        val lonLat = withContext(Dispatchers.Default) {
            projection?.undoProjection(relativeX, relativeY)
        } ?: doubleArrayOf(relativeX, relativeY)

        return lonLat
    }

    private suspend fun getNormalizedCoordinates(
        lat: Double,
        lon: Double,
        mapBounds: MapBounds,
        projection: Projection?
    ): DoubleArray {
        val projectedValues = withContext(Dispatchers.Default) {
            projection?.doProjection(lat, lon)
        } ?: doubleArrayOf(lon, lat)

        val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
        val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)

        return doubleArrayOf(x, y)
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    private fun deNormalize(t: Double, min: Double, max: Double): Double {
        return min + t * (max - min)
    }
}