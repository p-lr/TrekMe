package com.peterlaurence.trekme.core.georecord.domain.logic

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.georecord.domain.model.hasTrustedElevations
import com.peterlaurence.trekme.core.map.domain.models.Marker

fun getGeoStatistics(
    geoRecord: GeoRecord,
    visitor: (distance: Double, marker: Marker) -> Unit = { _, _ -> }
): GeoStatistics {
    var distanceOffset = 0.0
    val statCalculatorList = geoRecord.routeGroups.flatMap { it.routes }.map { route ->
        val distanceCalculator = distanceCalculatorFactory(geoRecord.hasTrustedElevations())
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
