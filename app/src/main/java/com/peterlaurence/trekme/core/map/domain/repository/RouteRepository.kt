package com.peterlaurence.trekme.core.map.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.RouteDao
import com.peterlaurence.trekme.core.map.domain.models.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeDao: RouteDao,
) {
    suspend fun importRoutes(map: Map) {
        routeDao.importRoutes(map)
    }

    suspend fun saveNewRoute(map: Map, route: Route) {
        routeDao.saveNewRoute(map, route)
    }

    suspend fun saveRouteInfo(map: Map, route: Route) {
        routeDao.saveRouteInfo(map, route)
    }

    suspend fun deleteRoute(map: Map, route: Route) {
        routeDao.deleteRoute(map, route)
    }

    suspend fun deleteRoutesUsingId(map: Map, ids: List<String>) {
        routeDao.deleteRoutesUsingId(map, ids)
    }
}
