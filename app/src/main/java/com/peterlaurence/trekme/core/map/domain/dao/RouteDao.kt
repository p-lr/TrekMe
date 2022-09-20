package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route

interface RouteDao {
    suspend fun importRoutes(map: Map)
    suspend fun saveNewRoute(map: Map, route: Route)
    suspend fun saveRouteInfo(map: Map, route: Route)
    suspend fun deleteRoute(map: Map, route: Route)
    suspend fun deleteRoutesUsingId(map: Map, ids: List<String>)
}