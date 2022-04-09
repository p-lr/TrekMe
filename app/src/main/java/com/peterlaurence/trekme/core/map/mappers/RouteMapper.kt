package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.data.RouteGson
import com.peterlaurence.trekme.core.map.data.RouteInfoKtx
import com.peterlaurence.trekme.core.map.data.RouteKtx

fun Route.toRouteKtx(): RouteKtx {
    val r = this
    return RouteKtx(
        markers = r.routeMarkers.map { it.toMarkerKtx() }
    )
}

fun Route.toRouteInfoKtx(): RouteInfoKtx {
    val r = this
    return RouteInfoKtx(
        id = r.id,
        name = r.name,
        color = r.color.value,
        visible = r.visible.value,
        elevationTrusted = r.elevationTrusted,
    )
}

fun RouteGson.Route.toDomain(): Route {
    val domainList = ArrayList<Marker>(routeMarkers.size)
    return Route(
        id = null,
        name = name,
        initialVisibility = visible,
        initialMarkers = routeMarkers.mapTo(domainList) { it.toDomain() },
        initialColor = color,
        elevationTrusted = elevationTrusted
    )
}