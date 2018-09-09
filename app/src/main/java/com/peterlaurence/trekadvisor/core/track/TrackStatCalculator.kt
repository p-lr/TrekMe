package com.peterlaurence.trekadvisor.core.track

import com.peterlaurence.trekadvisor.core.geotools.deltaTwoPoints
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint

/**
 * Calculates statistics for a track:
 * * distance
 * * TODO: height difference (max and stack)
 * * TODO: mean speed
 *
 * @author peterLaurence on 09/09/18
 */
class TrackStatCalculator {
    var distance: Double = 0.0

    private var lastTrackPoint: TrackPoint? = null

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        trkPtList.forEach { addTrackPoint(it) }
    }

    fun addTrackPoint(trkPt: TrackPoint) {
        updateDistance(trkPt)
    }

    /**
     * As the distance is computed incrementally, track points are considered near enough to use
     * rough (but fast) formulas.
     */
    private fun updateDistance(trkPt: TrackPoint) {
        if (lastTrackPoint != null) {
            val p1 = lastTrackPoint!!

            /* If we have elevation information for both points, use it */
            distance += if (p1.elevation != null && trkPt.elevation != null) {
                deltaTwoPoints(p1.latitude, p1.longitude, p1.elevation!!, trkPt.latitude,
                        trkPt.longitude, trkPt.elevation!!)
            } else {
                deltaTwoPoints(p1.latitude, p1.longitude, trkPt.latitude, trkPt.longitude)
            }
        }

        /* Update the last track point reference */
        lastTrackPoint = trkPt
    }
}