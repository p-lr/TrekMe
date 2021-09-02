package com.peterlaurence.trekme.core.map.gson

import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.domain.Marker

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

fun Marker.toEntity(): MarkerGson.Marker {
    val m = this
    return MarkerGson.Marker().apply {
        lat = m.lat
        lon = m.lon
        name = m.name
        elevation = m.elevation
        proj_x = m.proj_x
        proj_y = m.proj_y
        comment = m.comment
    }
}

fun MarkerGson.Marker.toDomain(): Marker {
    return Marker(lat, lon, name, elevation, proj_x, proj_y, comment)
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