package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class RemoveRouteInteractor @Inject constructor(
    private val routeRepository: RouteRepository
) {
    suspend fun removeRoutesOnMap(map: Map, routeIds: List<String>) {
        /* Remove in-memory routes now */
        map.routes.update {
            val routesToRemove = it.filter { r -> r.id in routeIds }.toSet()
            it - routesToRemove
        }

        /* Remove them on disk */
        routeRepository.deleteRoutesUsingId(map, routeIds)
    }
}