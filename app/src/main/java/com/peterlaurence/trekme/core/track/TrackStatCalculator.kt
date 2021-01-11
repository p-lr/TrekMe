package com.peterlaurence.trekme.core.track

import android.os.Parcelable
import com.peterlaurence.trekme.core.geotools.deltaTwoPoints
import com.peterlaurence.trekme.core.statistics.mean
import com.peterlaurence.trekme.util.gpx.model.Bounds
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.math.*

/**
 * Calculates statistics for a track:
 * * distance
 * * elevation difference (max and stack)
 * * duration
 * * bounds
 * * average speed
 *
 * Distance and elevation stack are computed using a rolling mean of [bufferSize] points and an
 * elevation threshold of 10 meters.
 *
 * Beware, [addTrackPoint] and [addTrackPointList] aren't synchronized.
 * A safe usage is to have one [TrackStatCalculator] instance per coroutine.
 * Yet, it is acceptable to invoke [addTrackPoint] from one thread before invoking [getStatistics]
 * from another thread.
 *
 * @author P.Laurence on 09/09/18
 */
class TrackStatCalculator {
    private var distance: Double = 0.0
    private var elevationDiffMax = 0.0
    private var elevationUpStack = 0.0
    private var elevationDownStack = 0.0
    private var durationInSecond = 0L
    private var avgSpeed = 0.0

    private var previousTrackPoint: TrackPoint? = null

    /* Duration statistic */
    private var firstPointTime: Long? = null

    /* Elevation statistics */
    private var lastKnownElevation: Double? = null
    private var lowestElevation: Double? = null
    private var highestElevation: Double? = null

    /* Bounds */
    private var minLat: Double? = null
    private var minLon: Double? = null
    private var maxLat: Double? = null
    private var maxLon: Double? = null

    /* Size of the buffer used to compute elevation mean */
    private val bufferSize = 5
    private val buffer = ArrayDeque<TrackPoint>()

    private var snapshot: Snapshot? = null

    fun getStatistics(): TrackStatistics {
        return TrackStatistics(distance, elevationDiffMax, elevationUpStack, elevationDownStack,
                durationInSecond, avgSpeed)
    }

    fun getBounds(): Bounds? {
        return Bounds(minLat ?: return null, minLon ?: return null,
                maxLat ?: return null, maxLon ?: return null)
    }

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        trkPtList.forEach { addTrackPoint(it) }
    }

    /**
     * The first 10 points are added to the buffer. The first time the buffer is full, we make a
     * snapshot. Then, subsequent snapshots are made only when the mean elevation is greater or
     * smaller than the previous elevation snapshot by 10 meters.
     * When we make a new snapshot, we update the distance and elevation stats accordingly.
     */
    fun addTrackPoint(trkPt: TrackPoint) {
        buffer.add(trkPt)
        val firstSnapshot = buffer.size == bufferSize

        /* Keep the buffer at its target size */
        if (buffer.size > bufferSize) buffer.poll()

        updateDistance(trkPt)
        updateDuration(trkPt)
        updateBounds(trkPt)
        updateMeanSpeed()

        if (buffer.size == bufferSize) {
            val elevations = buffer.mapNotNull { it.elevation }
            if (elevations.isNotEmpty()) {
                val meanEle = elevations.mean()
                val diffEle = snapshot?.let { abs(it.elevation - meanEle) }
                if (diffEle != null && diffEle > 10.0) {
                    distance = snapshot!!.distance + sqrt((distance - snapshot!!.distance).pow(2) + diffEle.pow(2))
                    this.snapshot = Snapshot(distance, meanEle)
                    updateElevationStats(meanEle)
                }
                if (firstSnapshot) {
                    snapshot = Snapshot(distance, meanEle)
                }
            }
        }

        /* Remember the previous track point */
        previousTrackPoint = trkPt
    }

    /**
     * As the distance is computed incrementally, track points are considered near enough to use
     * rough (but fast) formulas.
     */
    private fun updateDistance(trkPt: TrackPoint) {
        previousTrackPoint?.also { p ->
            distance += deltaTwoPoints(p.latitude, p.longitude, trkPt.latitude, trkPt.longitude)
        }
    }

    private fun updateElevationStats(ele: Double) {
        /* Lowest point update */
        lowestElevation?.also { eleLowest ->
            if (ele <= eleLowest) {
                lowestElevation = ele
            }
        } ?: { this.lowestElevation = ele }()

        /* Highest point update */
        highestElevation?.also { eleHighest ->
            if (ele >= eleHighest) {
                highestElevation = ele
            }
        } ?: { highestElevation = ele }()

        /* .. then we can update the elevation maximum difference*/
        highestElevation?.also { eleHighest ->
            lowestElevation?.also { eleLowest ->
                elevationDiffMax = eleHighest - eleLowest
            }
        }

        /* Elevation stack update */
        lastKnownElevation?.also { elePrevious ->
            val diff = abs(ele - elePrevious)
            if (ele > elePrevious) {
                elevationUpStack += diff
            } else if (ele < elePrevious) {
                elevationDownStack += diff
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
                if (time > origin) { // time should always be increasing, but who knows...
                    durationInSecond = (time - origin) / 1000
                }
            } ?: { firstPointTime = time }()
        }
    }

    private fun updateMeanSpeed() {
        if (durationInSecond != 0L) {
            avgSpeed = distance / durationInSecond
        }
    }

    private fun updateBounds(trkPt: TrackPoint) {
        minLat = min(trkPt.latitude, minLat ?: Double.MAX_VALUE)
        minLon = min(trkPt.longitude, minLon ?: Double.MAX_VALUE)
        maxLat = max(trkPt.latitude, maxLat ?: Double.MIN_VALUE)
        maxLon = max(trkPt.longitude, maxLon ?: Double.MIN_VALUE)
    }
}

private data class Snapshot(val distance: Double, val elevation: Double)

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
data class TrackStatistics(val distance: Double, var elevationDifferenceMax: Double,
                           val elevationUpStack: Double, val elevationDownStack: Double,
                           val durationInSecond: Long? = null, val avgSpeed: Double? = null) : Parcelable