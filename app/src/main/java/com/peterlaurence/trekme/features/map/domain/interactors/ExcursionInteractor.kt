package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinate
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.ExcursionWaypointWithNormalizedPos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExcursionInteractor @Inject constructor(
    private val excursionRepository: ExcursionRepository,
) {
    /**
     * For each [ExcursionRef] corresponds potentially a list of [Route].
     */
    suspend fun loadRoutes(refs: List<ExcursionRef>): kotlin.collections.Map<ExcursionRef, List<Route>> {
        val routesForRef = mutableMapOf<ExcursionRef, List<Route>>()
        for (ref in refs) {
            val geoRecord = excursionRepository.getGeoRecord(ref.id) ?: continue
            routesForRef[ref] = geoRecord.routeGroups.flatMap { group ->
                group.routes
            }
        }
        return routesForRef
    }

    suspend fun getWaypointsFlow(ref: ExcursionRef, map: Map): Flow<List<ExcursionWaypointWithNormalizedPos>> {
        val waypoints = excursionRepository.getWaypoints(ref.id) ?: return emptyFlow()

        return waypoints.map {
            it.map { wpt ->
                val (x, y) = getNormalizedCoordinates(
                    wpt.latitude,
                    wpt.longitude,
                    map.mapBounds,
                    map.projection
                )

                ExcursionWaypointWithNormalizedPos(wpt, x, y)
            }
        }
    }

    suspend fun updateAndSaveWaypoint(excursionId: String, map: Map, waypoint: ExcursionWaypoint, x: Double, y: Double) {
        val mapBounds = map.mapBounds

        val lonLat = getLonLatFromNormalizedCoordinate(x, y, map.projection, mapBounds)

        excursionRepository.updateWaypoint(excursionId, waypoint, lonLat[1], lonLat[0])
    }

    suspend fun updateAndSaveWaypoint(excursionId: String, waypoint: ExcursionWaypoint, name: String, lat: Double?, lon: Double?, comment: String) {
        if (name != waypoint.name || lat != null || lon != null || comment != waypoint.comment) {
            excursionRepository.updateWaypoint(excursionId, waypoint, name, lat, lon, comment)
        }
    }

    suspend fun deleteWaypoint(excursionId: String, waypoint: ExcursionWaypoint) {
        excursionRepository.deleteWaypoint(excursionId, waypoint)
    }
}