package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import javax.inject.Inject

class RouteInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val routeRepository: RouteRepository
) {
    suspend fun removeRoutesOnMaps(routeIds: List<String>) {
        /* Remove in-memory routes now */
        mapRepository.getCurrentMapList().forEach { map ->
            map.routes.value.filter { it.id in routeIds }.forEach { route ->
                map.deleteRoute(route)
            }
        }

        /* Remove them on disk */
        mapRepository.getCurrentMapList().forEach { map ->
            routeRepository.deleteRoutesUsingId(map, routeIds)
        }
    }
}