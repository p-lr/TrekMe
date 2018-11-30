package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.statistics.hpfilter
import com.peterlaurence.trekme.util.gpx.model.TrackSegment

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