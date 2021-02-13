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
class TrackStatCalculator(private val distanceCalculator: DistanceCalculator) {
    private var elevationDiffMax = 0.0
    private var elevationUpStack = 0.0
    private var elevationDownStack = 0.0
    private var durationInSecond = 0L
    private var avgSpeed = 0.0


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

    fun getStatistics(): TrackStatistics {
        return TrackStatistics(distanceCalculator.getDistance(), elevationDiffMax, elevationUpStack, elevationDownStack,
                durationInSecond, avgSpeed)
    }

    fun getBounds(): Bounds? {
        return Bounds(minLat ?: return null, minLon ?: return null,
                maxLat ?: return null, maxLon ?: return null)
    }

    fun addTrackPointList(trkPtList: List<TrackPoint>) {
        trkPtList.forEach { addTrackPoint(it) }
    }

    fun addTrackPoint(trkPt: TrackPoint) {
        distanceCalculator.addPoint(trkPt.latitude, trkPt.longitude, trkPt.elevation) { ele ->
            updateElevationStats(ele)
        }

        updateDuration(trkPt)
        updateBounds(trkPt)
        updateMeanSpeed()
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
            avgSpeed = distanceCalculator.getDistance() / durationInSecond
        }
    }

    private fun updateBounds(trkPt: TrackPoint) {
        minLat = min(trkPt.latitude, minLat ?: Double.MAX_VALUE)
        minLon = min(trkPt.longitude, minLon ?: Double.MAX_VALUE)
        maxLat = max(trkPt.latitude, maxLat ?: Double.MIN_VALUE)
        maxLon = max(trkPt.longitude, maxLon ?: Double.MIN_VALUE)
    }
}

interface DistanceCalculator {
    fun getDistance(): Double
    fun addPoint(lat: Double, lon: Double, ele: Double?, onEleSnapShot: ((Double) -> Unit)? = null)
}

/**
 * When we trust the values of elevations, we use a lower threshold.
 */
class DistanceCalculatorImpl(isElevationTrusted: Boolean) : DistanceCalculator {
    private val eleThreshold: Int = if (isElevationTrusted) 3 else 10
    private var distance: Double = 0.0

    /* Size of the buffer used to compute elevation mean */
    private val bufferSize = 5
    private val buffer = ArrayDeque<Double>()

    private var previousLat: Double? = null
    private var previousLon: Double? = null
    private var snapshot: Snapshot? = null

    override fun getDistance(): Double {
        return distance
    }

    /**
     * The first points are added to the buffer. The first time the buffer is full, we make a
     * snapshot. Then, subsequent snapshots are made only when the mean elevation is greater or
     * smaller than the previous elevation snapshot by 10 meters.
     * When we make a new snapshot, the distance is updated and the provided [onEleSnapShot] callback
     * is invoked with the mean elevation. This callback can be used to update elevation statistics.
     */
    override fun addPoint(lat: Double, lon: Double, ele: Double?, onEleSnapShot: ((Double) -> Unit)?) {
        buffer.add(ele ?: Double.NaN)
        val firstSnapshot = buffer.size == bufferSize

        /* Keep the buffer at its target size */
        if (buffer.size > bufferSize) buffer.poll()

        updateDistance(lat, lon)

        if (buffer.size == bufferSize) {
            val elevations = buffer.filterNot { it.isNaN() }
            if (elevations.isNotEmpty()) {
                val meanEle = elevations.mean()
                val diffEle = snapshot?.let { abs(it.elevation - meanEle) }
                if (diffEle != null && diffEle > eleThreshold) {
                    distance = snapshot!!.distance + sqrt((distance - snapshot!!.distance).pow(2) + diffEle.pow(2))
                    this.snapshot = Snapshot(distance, meanEle)
                    onEleSnapShot?.invoke(meanEle)
                }
                if (firstSnapshot) {
                    snapshot = Snapshot(distance, meanEle)
                }
            }
        }
    }

    /**
     * As the distance is computed incrementally, points are considered near enough to use
     * rough (but fast) formulas.
     */
    private fun updateDistance(lat: Double, lon: Double) {
        val prevLat = previousLat
        val prevLon = previousLon
        if (prevLat != null && prevLon != null) {
            distance += deltaTwoPoints(prevLat, prevLon, lat, lon)
        }
        previousLat = lat
        previousLon = lon
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