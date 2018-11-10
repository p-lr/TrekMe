package com.peterlaurence.trekadvisor.core.track

import com.peterlaurence.trekadvisor.core.statistics.hpfilter
import com.peterlaurence.trekadvisor.util.gpx.model.TrackSegment

/**
 * Apply Hodrick–Prescott filter onto the elevations.
 */
fun TrackSegment.hpFilter() {
    val trackPointsWithElevation = trackPoints.filter { it.elevation != null }
    val elevations = trackPointsWithElevation.map { it.elevation!! }.toDoubleArray()
    if (elevations.isNotEmpty()) {
        val elevationsFiltered = hpfilter(elevations)
        trackPointsWithElevation.forEachIndexed { index, trackPoint ->
            trackPoint.elevation = elevationsFiltered[index]
        }
    }
}