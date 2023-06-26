package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.data.models.RouteInfoKtx
import com.peterlaurence.trekme.core.map.data.models.RouteKtx

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
        name = r.name.value,
        color = r.color.value,
        visible = r.visible.value,
        elevationTrusted = r.elevationTrusted,
    )
}
