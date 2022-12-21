package com.peterlaurence.trekme.core.georecord.domain.logic

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.hasTrustedElevations

fun getGeoStatistics(geoRecord: GeoRecord): GeoStatistics {
    val statCalculatorList = geoRecord.routeGroups.flatMap { it.routes }.map { route ->
        val distanceCalculator = distanceCalculatorFactory(geoRecord.hasTrustedElevations())
        val statCalculator = TrackStatCalculator(distanceCalculator)
        route.routeMarkers.forEach {
            statCalculator.addTrackPoint(it.lat, it.lon, it.elevation, it.time)
        }
        statCalculator
    }

    return statCalculatorList.mergeStats()
}
