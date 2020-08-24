package com.peterlaurence.trekme.core.track

import android.os.Parcelable
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
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

    private var lastTrackPoint: TrackPoint? = null

    /* Duration statistic */
    private var firstPointTime: Long? = null

    /* Elevation statistics */
    private var firstElevationReceived = false
    private var firstElevation: Double? = null
    private var lastKnownElevation: Double? = null
    private var lowestPoint: TrackPoint? = null
    private var highestPoint: TrackPoint? = null

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
        lastTrackPoint?.also { p ->
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
        val ele = trkPt.elevation ?: return

        /* Filter out the first point with elevation information -- not trusted */
        if (!firstElevationReceived || ele == firstElevation) {
            firstElevation = ele
            firstElevationReceived = true
            return
        }

        /* Lowest point update */
        lowestPoint?.elevation?.also { eleLowest ->
            if (ele <= eleLowest) {
                lowestPoint = trkPt
            }
        } ?: { this.lowestPoint = trkPt }()

        /* Highest point update */
        highestPoint?.elevation?.also { eleHighest ->
            if (ele >= eleHighest) {
                highestPoint = trkPt
            }
        } ?: { highestPoint = trkPt }()

        /* .. then we can update the elevation maximum difference*/
        highestPoint?.elevation?.also { eleHighest ->
            lowestPoint?.elevation?.also { eleLowest ->
                trackStatistics.elevationDifferenceMax = eleHighest - eleLowest
            }
        }

        /* Elevation stack update */
        lastKnownElevation?.also { elePrevious ->
            val diff = abs(ele - elePrevious)
            if (ele > elePrevious) {
                trackStatistics.elevationUpStack += diff
            } else if (ele < elePrevious) {
                trackStatistics.elevationDownStack += diff
            }
        }
        lastKnownElevation = ele
    }

    /**
     * Remember the [Date] of the first track point, and use it as reference to get the duration.
     */
    private fun updateDuration(trkPt: TrackPoint) {
        trkPt.time?.also { time ->
            firstPointTime?.also { origin ->
                trackStatistics.durationInSecond = (time - origin) / 1000
            } ?: { firstPointTime = time }()
        }
    }
}

/**
 * Container for statistics of a track.
 *
 * @param distance The distance in meters
 * @param elevationUpStack The cumulative elevation up in meters
 * @param elevationDownStack The cumulative elevation down in meters
 * @param elevationDifferenceMax The difference between the highest and lowest altitude
 * @param durationInSecond The total time in seconds
 */
@Parcelize
data class TrackStatistics(var distance: Double, var elevationDifferenceMax: Double,
                           var elevationUpStack: Double, var elevationDownStack: Double,
                           var durationInSecond: Long) : Parcelable