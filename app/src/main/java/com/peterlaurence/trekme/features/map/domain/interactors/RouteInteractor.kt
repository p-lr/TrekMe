package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.MarkerWithNormalizedPos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class RouteInteractor @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    suspend fun loadRoutes(map: Map) {
        routeRepository.importRoutes(map)
    }

    fun getExistingMarkerPositions(map: Map, route: Route): Flow<MarkerWithNormalizedPos> {
        return route.routeMarkers.asFlow().toNormalizedPositions(map)
    }

    fun getLiveMarkerPositions(map: Map, route: Route): Flow<MarkerWithNormalizedPos> {
        return route.routeMarkersFlow.toNormalizedPositions(map)
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