package com.peterlaurence.trekme.features.map.domain.interactors

import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.models.BeaconWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.LandmarkWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

class MapInteractor @Inject constructor(
    private val markersDao: MarkersDao,
    private val landmarksDao: LandmarksDao,
    private val mapRepository: MapRepository,
    private val routeRepository: RouteRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Create and add a new landmark.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun addLandmark(map: Map, x: Double, y: Double): Landmark =
        withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)
            val name = context.getString(R.string.landmark_default_name)

            Landmark(name, lonLat[1], lonLat[0]).also {
                map.addLandmark(it)
            }
        }

    /**
     * Create and add a new marker.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun addMarker(map: Map, x: Double, y: Double): Marker =
        withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)

            Marker(lonLat[1], lonLat[0]).also {
                map.addMarker(it)
            }
        }

    suspend fun makeBeacon(map: Map, x: Double, y: Double): Beacon {
        return withContext(scope.coroutineContext) {
            val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, map.mapBounds)
            val name = context.getString(R.string.beacon_default_name)
            // TODO fetch last radius from data store

            Beacon(id = UUID.randomUUID().toString(), name = name, lat = lonLat[1], lon = lonLat[0])
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

        landmarksDao.saveLandmarks(map)
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

        markersDao.saveMarkers(map)
    }

    /**
     * Update the marker position and save.
     * [x] and [y] are expected to be normalized coordinates.
     */
    suspend fun updateAndSaveBeacon(beacon: Beacon, map: Map, x: Double, y: Double): Beacon {
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        val updatedBeacon = beacon.copy(lat = lonLat[1], lon = lonLat[0])

        map.beacons.update { formerList ->
            formerList.filter { it.id != beacon.id } + updatedBeacon
        }

        scope.launch {
            // TODO: save beacon
        }

        return updatedBeacon
    }

    fun deleteLandmark(landmark: Landmark, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        map.deleteLandmark(landmark)
        landmarksDao.saveLandmarks(map)
    }

    fun deleteMarker(marker: Marker, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        map.deleteMarker(marker)
        markersDao.saveMarkers(map)
    }

    fun deleteBeacon(beacon: Beacon, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch

        map.beacons.update { formerList ->
            formerList.filter { it.id != beacon.id }
        }

        // TODO: effectively delete beacon on data layer
    }

    /**
     * Save all markers (debounced).
     */
    fun saveMarkers(mapId: UUID) {
        updateMarkerJob?.cancel()
        updateMarkerJob = scope.launch {
            delay(1000)
            val map = mapRepository.getMap(mapId) ?: return@launch
            markersDao.saveMarkers(map)
        }
    }

    suspend fun getBeaconPositionsFlow(map: Map): Flow<List<BeaconWithNormalizedPos>> {
        return map.beacons.map { beaconList ->
            beaconList.map { beacon ->
                val (x, y) = getNormalizedCoordinates(
                    beacon.lat,
                    beacon.lon,
                    map.mapBounds,
                    map.projection
                )

                BeaconWithNormalizedPos(beacon, x, y)
            }
        }
    }

    /**
     * Given a [Map], get the list of [Landmark] along with their normalized position.
     */
    suspend fun getLandmarkPositions(map: Map): List<LandmarkWithNormalizedPos> {
        /* Import landmarks */
        landmarksDao.getLandmarksForMap(map)

        val landmarks = map.landmarks ?: return emptyList()

        return landmarks.map { landmark ->
            val (x, y) = getNormalizedCoordinates(
                landmark.lat,
                landmark.lon,
                map.mapBounds,
                map.projection
            )

            LandmarkWithNormalizedPos(landmark, x, y)
        }
    }

    suspend fun getMarkerPositions(map: Map): List<MarkerWithNormalizedPos> {
        /* Import markers */
        markersDao.getMarkersForMap(map)

        val markers = map.markers ?: return emptyList()
        return markers.map { marker ->
            val (x, y) = getNormalizedCoordinates(
                marker.lat,
                marker.lon,
                map.mapBounds,
                map.projection
            )

            MarkerWithNormalizedPos(marker, x, y)
        }
    }

    suspend fun getMarkerPosition(map: Map, marker: Marker): MarkerWithNormalizedPos {
        val (x, y) = getNormalizedCoordinates(
            marker.lat,
            marker.lon,
            map.mapBounds,
            map.projection
        )

        return MarkerWithNormalizedPos(marker, x, y)
    }

    suspend fun loadRoutes(map: Map) {
        routeRepository.importRoutes(map)
    }

    fun getExistingMarkerPositions(map: Map, route: Route): Flow<MarkerWithNormalizedPos> {
        return route.routeMarkers.asFlow().toNormalizedPositions(map)
    }

    fun getLiveMarkerPositions(map: Map, route: Route): Flow<MarkerWithNormalizedPos> {
        return route.routeMarkersFlow.toNormalizedPositions(map)
    }

    suspend fun getNormalizedCoordinates(
        map: Map,
        latitude: Double,
        longitude: Double
    ): NormalizedPos {
        return getNormalizedCoordinates(latitude, longitude, map.mapBounds, map.projection).let {
            NormalizedPos(it[0], it[1])
        }
    }

    fun getMapFullWidthDistance(map: Map): Double? {
        val projection = map.projection
        val b = map.mapBounds
        return if (projection != null) {
            val (lon1, lat1) = projection.undoProjection(b.X0, b.Y0) ?: return null
            val (lon2, lat2) = projection.undoProjection(b.X1, b.Y0) ?: return null
            distanceApprox(lat1, lon1, lat2, lon2)
        } else {
            distanceApprox(b.Y0, b.X0, b.Y0, b.X1)
        }
    }

    private fun Flow<Marker>.toNormalizedPositions(map: Map): Flow<MarkerWithNormalizedPos> {
        return mapNotNull { marker ->
            val (x, y) = getNormalizedCoordinates(
                marker.lat,
                marker.lon,
                map.mapBounds,
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

    private var updateMarkerJob: Job? = null
}