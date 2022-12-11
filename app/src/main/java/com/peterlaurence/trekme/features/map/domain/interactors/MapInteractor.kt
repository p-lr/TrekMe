package com.peterlaurence.trekme.features.map.domain.interactors

import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.core.map.domain.models.Landmark
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.LandmarkWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class MapInteractor @Inject constructor(
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

    fun deleteLandmark(landmark: Landmark, mapId: UUID) = scope.launch {
        val map = mapRepository.getMap(mapId) ?: return@launch
        map.deleteLandmark(landmark)
        landmarksDao.saveLandmarks(map)
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
}