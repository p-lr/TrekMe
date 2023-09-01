package com.peterlaurence.trekme.features.map.domain.core

import com.peterlaurence.trekme.core.location.domain.model.Location
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

/**
 * Given a [TrackVicinityVerifier], and a location, the algorithm defines whether an alert should be
 * fired or not.
 */
@OptIn(ExperimentalTime::class)
class TrackVicinityAlgorithm(private val trackVicinityVerifier: TrackVicinityVerifier) {

    private val timeSource = TimeSource.Monotonic
    private var lastAlertTime: TimeSource.Monotonic.ValueTimeMark? = null

    /**
     * If the location isn't in the vicinity, fire an alert if the last alert occurred more than 20
     * seconds ago.
     */
    suspend fun processLocation(location: Location, threshold: Int): Boolean {
        val isInside = trackVicinityVerifier.isInVicinity(location.latitude, location.longitude, threshold)
        return if (!isInside) {
            val mark = timeSource.markNow()
            lastAlertTime?.let { lastMark ->
                val shouldAlert = (mark - lastMark).inWholeSeconds > 20
                if (shouldAlert) lastAlertTime = mark
                shouldAlert
            } ?: run {
                lastAlertTime = mark
                true
            }
        } else false
    }
}