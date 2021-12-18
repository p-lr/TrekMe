package com.peterlaurence.trekme.features.map.domain.interactors

import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapBounds
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.models.LandmarkWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
import com.peterlaurence.trekme.features.map.presentation.ui.components.LandMark
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

class MapInteractor @Inject constructor(
    private val mapLoader: MapLoader,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Create and add a new landmark.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun addLandmark(map: Map, x: Double, y: Double): Landmark = withContext(scope.coroutineContext) {
        val mapBounds = map.mapBounds
        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)
        val name =  context.getString(R.string.landmark_default_name)

        Landmark(name, lonLat[1], lonLat[0]).also {
            map.addLandmark(it)
        }
    }

    /**
     * Create and add a new marker.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun addMarker(map: Map, x: Double, y: Double): Marker = withContext(scope.coroutineContext) {
        val mapBounds = map.mapBounds
        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        Marker(lonLat[1], lonLat[0]).also {
            map.addMarker(it)
        }
    }

    /**
     * Update the landmark position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    fun updateAndSaveLandmark(landmark: Landmark, map: Map, x: Double, y: Double) = scope.launch {
        val mapBounds = map.mapBounds

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
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        marker.apply {
            lat = lonLat[1]
            lon = lonLat[0]
        }

        mapLoader.saveMarkers(map)
    }

    /**
     * Save all markers (debounced).
     */
    fun saveMarkers(mapId: Int) {
        updateMarkerJob?.cancel()
        updateMarkerJob = scope.launch {
            delay(1000)
            val map = mapLoader.getMap(mapId) ?: return@launch
            mapLoader.saveMarkers(map)
        }
    }

    /**
     * Given a [Map], get the list of [Landmark] along with their normalized position.
     */
    suspend fun getLandmarkPositions(map: Map): List<LandmarkWithNormalizedPos> {
        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)

        val landmarks = map.landmarks ?: return emptyList()
        val mapBounds = map.mapBounds

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
        val mapBounds = map.mapBounds

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

    suspend fun getMarkerPosition(map: Map, marker: Marker): MarkerWithNormalizedPos? {
        val mapBounds = map.mapBounds

        val (x, y) = getNormalizedCoordinates(
            marker.lat,
            marker.lon,
            mapBounds,
            map.projection
        )

        return MarkerWithNormalizedPos(marker, x, y)
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

    private var updateMarkerJob: Job? = null
}