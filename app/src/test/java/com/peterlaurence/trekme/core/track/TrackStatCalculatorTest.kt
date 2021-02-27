package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TrackStatCalculatorTest {
    private val trackPoints = listOf(
            // First identical 10 points (see elevation test)
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, null),
            TrackPoint(50.3, 48.7, 10.0),
            TrackPoint(50.4, 48.75, 155.0),
            TrackPoint(50.4, 48.75, null),
            TrackPoint(50.45, 48.65, -20.0),
            TrackPoint(50.5, 48.6, 200.0),
            TrackPoint(50.5, 48.6, null)
    )

    private lateinit var statCalculator: TrackStatCalculator

    @Before
    fun init() {
        statCalculator = TrackStatCalculator(DistanceCalculatorImpl(false))
    }

    /**
     * The reference used is this [website](https://gps-coordinates.org/distance-between-coordinates.php)
     */
    @Test
    fun distanceTest() {
        statCalculator.addTrackPointList(trackPoints)

        /* Tolerate 15m error */
        assertEquals(27_267.0, statCalculator.getStatistics().distance, 15.0)
    }

    /**
     * TODO: Update this test to adapt to latest smoothing algorithm.
     */
    @Test
    fun elevationTest() {
        statCalculator.addTrackPointList(trackPoints)
        val stats = statCalculator.getStatistics()

        assertEquals(0.0, stats.elevationDifferenceMax, 0.0)
        assertEquals(0.0, stats.elevationUpStack, 0.1)
        assertEquals(0.0, stats.elevationDownStack, 0.1)
    }
}