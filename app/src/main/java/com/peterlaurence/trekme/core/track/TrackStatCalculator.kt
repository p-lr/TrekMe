package com.peterlaurence.trekme.core.track

import android.os.Parcelable
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.core.statistics.mean
import com.peterlaurence.trekme.core.statistics.rolling
import com.peterlaurence.trekme.util.gpx.model.Bounds
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.android.parcel.Parcelize
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Calculates statistics for a track:
 * * distance
 * * elevation difference (max and stack)
 * * duration
 * * bounds
 * * average speed
 *
 * @author P.Laurence on 09/09/18
 */
class TrackStatCalculator {
    private val trackStatistics = TrackStatistics(0.0, 0.0, 0.0, 0.0)

    private var lastTrackPoint: TrackPoint? = null

    /* Duration statistic */
    private var firstPointTime: Long? = null

    /* Bounds */
    private var minLat: Double? = null
    private var minLon: Double? = null
    private var maxLat: Double? = null
    private var maxLon: Double? = null

    private val elevations = mutableListOf<Double>()

    fun getStatistics(): TrackStatistics {
        return trackStatistics
    }

    fun getBounds(): Bounds? {
        return Bounds(minLat ?: return null, minLon ?: return null,
                maxLat ?: return null, maxLon ?: return null)
    }

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        /* Update all but elevations stats (they'll be computed using all points) */
        trkPtList.forEach {
            updateDistance(it)
            updateDuration(it)
            updateBounds(it)
            updateMeanSpeed()
        }

        /* Elevation stats */
        val elevations = trkPtList.mapNotNull { it.elevation }
        val smoothedElevations = elevations.rolling(10) {
            it.mean()
        }
        computeElevationStats(smoothedElevations)
    }

    fun addTrackPoint(trkPt: TrackPoint) {
        updateDistance(trkPt)
        updateElevationStatistics(trkPt)
        updateDuration(trkPt)
        updateBounds(trkPt)
        updateMeanSpeed()
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

        elevations.add(ele)
        val smoothedElevations = elevations.rolling(10) {
            it.mean()
        }

        computeElevationStats(smoothedElevations)
    }

    private fun computeElevationStats(smoothedElevations: List<Double>) {
        /* Lowest point */
        val lowestPoint = smoothedElevations.minOrNull()

        /* Highest point update */
        val highestPoint = smoothedElevations.maxOrNull()

        /* .. then we can update the elevation maximum difference*/
        if (lowestPoint != null && highestPoint != null) {
            trackStatistics.elevationDifferenceMax = highestPoint - lowestPoint
        }

        /* Elevation stack update */
        if (smoothedElevations.isEmpty()) return
        var eleUpStack = 0.0
        var eleDownStack = 0.0
        smoothedElevations.reduce { elePrevious, d ->
            val diff = abs(d - elePrevious)
            if (d > elePrevious) {
                eleUpStack += diff
            } else if (d < elePrevious) {
                eleDownStack += diff
            }
            d
        }
        trackStatistics.elevationUpStack = eleUpStack
        trackStatistics.elevationDownStack = eleDownStack
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

    private fun updateMeanSpeed() {
        trackStatistics.durationInSecond?.also { duration ->
            if (duration != 0L) {
                trackStatistics.avgSpeed = trackStatistics.distance / duration
            }
        }
    }

    private fun updateBounds(trkPt: TrackPoint) {
        minLat = min(trkPt.latitude, minLat ?: Double.MAX_VALUE)
        minLon = min(trkPt.longitude, minLon ?: Double.MAX_VALUE)
        maxLat = max(trkPt.latitude, maxLat ?: Double.MIN_VALUE)
        maxLon = max(trkPt.longitude, maxLon ?: Double.MIN_VALUE)
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
 * @param avgSpeed The average speed in meters per seconds
 */
@Parcelize
data class TrackStatistics(var distance: Double, var elevationDifferenceMax: Double,
                           var elevationUpStack: Double, var elevationDownStack: Double,
                           var durationInSecond: Long? = null, var avgSpeed: Double? = null) : Parcelable