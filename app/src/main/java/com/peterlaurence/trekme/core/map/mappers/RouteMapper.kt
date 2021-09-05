package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.entity.RouteGson

fun Route.toEntity(): RouteGson.Route {
    val r = this
    return RouteGson.Route().apply {
        id = r.id
        name = r.name
        color = r.color
        visible = r.visible
        elevationTrusted = r.elevationTrusted
        routeMarkers = r.routeMarkers.map { it.toEntity() }
    }
}

fun RouteGson.Route.toDomain(): Route {
    val domainList = ArrayList<Marker>(routeMarkers.size)
    return Route(
        id,
        name,
        visible,
        routeMarkers = routeMarkers.mapTo(domainList) { it.toDomain() },
        color = color,
        elevationTrusted = elevationTrusted
    )
}