package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.data.models.RouteInfoKtx
import com.peterlaurence.trekme.core.map.data.models.RouteKtx

fun Route.toRouteKtx(): RouteKtx {
    return RouteKtx(
        markers = routeMarkers.map { it.toMarkerKtx() }
    )
}

fun Route.toRouteInfoKtx(): RouteInfoKtx {
    return RouteInfoKtx(
        id = id,
        name = name.value,
        color = color.value,
        visible = visible.value,
        elevationTrusted = elevationTrusted,
    )
}
