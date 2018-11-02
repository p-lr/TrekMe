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
    private var lastKnownElevation: Double? = null
    private lateinit var lowestPoint: TrackPoint
    private lateinit var highestPoint: TrackPoint

    fun getStatistics(): TrackStatistics {
        return trackStatistics
    }

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        trkPtList.forEach { addTrackPoint(it) }
    }

    fun addTrackPoint(trkPt: TrackPoint) {
        updateDistance(trkPt)
        updateElevationStatistics(trkPt)
        updateDuration(trkPt)
    }

    /**
     * As the distance is computed incrementally, track points are considered near enough to use
     * rough (but fast) formulas.
     */
    private fun updateDistance(trkPt: TrackPoint) {
        if (::lastTrackPoint.isInitialized) {
            val p = lastTrackPoint

            /* If we have elevation information for both points, use it */
            trackStatistics.distance += if (p.elevation != null && trkPt.elevation != null) {
                deltaTwoPoints(p.latitude, p.longitude, p.elevation!!, trkPt.latitude,
                        trkPt.longitude, trkPt.elevation!!)
            } else {
                deltaTwoPoints(p.latitude, p.longitude, trkPt.latitude, trkPt.longitude)
            }
        }

        /* Update the last track point reference */
        lastTrackPoint = trkPt
    }

    private fun updateElevationStatistics(trkPt: TrackPoint) {
        if (trkPt.elevation != null) {
            val ele = trkPt.elevation!!

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
            if (lastKnownElevation != null) {
                val diff = abs(ele - lastKnownElevation!!)
                if (ele > lastKnownElevation!!) {
                    trackStatistics.elevationUpStack += diff
                } else {
                    trackStatistics.elevationDownStack += diff
                }
            }
            lastKnownElevation = trkPt.elevation!!
        }
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