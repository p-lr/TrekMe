package com.peterlaurence.trekadvisor.core.track

import android.os.Parcelable
import com.peterlaurence.trekadvisor.core.geotools.deltaTwoPoints
import com.peterlaurence.trekadvisor.util.gpx.model.TrackPoint
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.math.abs

/**
 * Calculates statistics for a track:
 * * distance
 * * elevation difference (max and stack)
 * * duration
 * * TODO: mean speed
 *
 * @author peterLaurence on 09/09/18
 */
class TrackStatCalculator {
    private val trackStatistics = TrackStatistics(0.0, 0.0, 0.0, 0.0, 0)

    private lateinit var lastTrackPoint: TrackPoint

    /* Duration statistic */
    private var firstPointTime: Long? = null

    /* Elevation statistics */
    private var firstElevationReceived = false
    private var firstElevation: Double? = null
    private var lastTrustedElevation: Double? = null
    private var lastKnownElevation: Double? = null
    private var descendingCount: Int = 0
    private var ascendingCount: Int = 0
    private lateinit var lowestPoint: TrackPoint
    private lateinit var highestPoint: TrackPoint

    fun getStatistics(): TrackStatistics {
        return trackStatistics
    }

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        trkPtList.forEach { addTrackPoint(it) }
    }

    fun addTrackPoint(trkPt: TrackPoint) {
        updateElevationStatistics(trkPt).also {
            updateDistance(trkPt, it)
        }
        updateDuration(trkPt)
    }

    /**
     * As the distance is computed incrementally, track points are considered near enough to use
     * rough (but fast) formulas.
     */
    private fun updateDistance(trkPt: TrackPoint, useElevation: Boolean) {
        if (::lastTrackPoint.isInitialized) {
            val p = lastTrackPoint

            /* If we have elevation information for both points, use it */
            trackStatistics.distance += if (p.elevation != null && trkPt.elevation != null && useElevation) {
                deltaTwoPoints(p.latitude, p.longitude, p.elevation!!, trkPt.latitude,
                        trkPt.longitude, trkPt.elevation!!)
            } else {
                deltaTwoPoints(p.latitude, p.longitude, trkPt.latitude, trkPt.longitude)
            }
        }

        /* Update the last track point reference */
        lastTrackPoint = trkPt
    }

    private fun updateElevationStatistics(trkPt: TrackPoint): Boolean {
        var elevationTrusted = false
        if (trkPt.elevation != null) {
            val ele = trkPt.elevation!!

            /* Filter out the first point with elevation information -- not trusted */
            if (!firstElevationReceived || ele == firstElevation) {
                firstElevation = ele
                firstElevationReceived = true
                return false
            }

            /* Lowest point update */
            if (::lowestPoint.isInitialized) {
                if (ele <= lowestPoint.elevation!!) {
                    this.lowestPoint = trkPt
                }
            } else {
                this.lowestPoint = trkPt
            }

            /* Highest point update */
            if (::highestPoint.isInitialized) {
                if (ele >= highestPoint.elevation!!) {
                    this.highestPoint = trkPt
                }
            } else {
                this.highestPoint = trkPt
            }

            /* .. then we can update the elevation maximum difference*/
            trackStatistics.elevationDifferenceMax = highestPoint.elevation!! - lowestPoint.elevation!!

            /* Elevation stack update */
            if (lastTrustedElevation != null) {
                val diff = abs(ele - lastTrustedElevation!!)
                if (ele > lastKnownElevation!!) {
                    ascendingCount++
                    descendingCount = 0
                    if (ascendingCount > 3) {
                        elevationTrusted = true
                        lastTrustedElevation = ele
                        trackStatistics.elevationUpStack += diff
                    }

                } else if (ele < lastKnownElevation!!) {
                    descendingCount++
                    ascendingCount = 0
                    if (descendingCount > 3) {
                        elevationTrusted = true
                        lastTrustedElevation = ele
                        trackStatistics.elevationDownStack += diff
                    }
                }
            } else {
                lastTrustedElevation = ele
            }
            lastKnownElevation = ele
        }
        return elevationTrusted
    }

    /**
     * Remember the [Date] of the first track point, and use it as reference to get the duration.
     */
    private fun updateDuration(trktPt: TrackPoint) {
        trktPt.time?.let {
            if (firstPointTime == null) {
                firstPointTime = trktPt.time
            } else {
                trackStatistics.durationInSecond = (it - firstPointTime!!) / 1000
            }
        }
    }
}

@Parcelize
data class TrackStatistics(var distance: Double, var elevationDifferenceMax: Double,
                           var elevationUpStack: Double, var elevationDownStack: Double,
                           var durationInSecond: Long) : Parcelable