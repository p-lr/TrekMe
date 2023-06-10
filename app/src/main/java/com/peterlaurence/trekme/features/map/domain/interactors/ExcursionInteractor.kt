package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Route
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
}