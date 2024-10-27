package com.peterlaurence.trekme.core.georecord.domain.logic

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route

fun getGeoStatistics(
    geoRecord: GeoRecord,
    visitor: (distance: Double, marker: Marker) -> Unit = { _, _ -> }
): GeoStatistics {
    val routes = geoRecord.routeGroups.flatMap { it.routes }
    return getGeoStatistics(routes, visitor)
}

fun getGeoStatistics(
    routes: List<Route>,
    visitor: (distance: Double, marker: Marker) -> Unit = { _, _ -> }
): GeoStatistics {
    var distanceOffset = 0.0
    val statCalculatorList = routes.map { route ->
        val distanceCalculator = distanceCalculatorFactory(route.elevationTrusted)
        val statCalculator = TrackStatCalculator(distanceCalculator)
        var distance = 0.0
        route.routeMarkers.forEach {
            statCalculator.addTrackPoint(it.lat, it.lon, it.elevation, it.time)
            distance = distanceCalculator.getDistance()
            visitor(distance + distanceOffset, it)
        }
        distanceOffset += distance
        statCalculator
    }

    return statCalculatorList.mergeStats()
}
