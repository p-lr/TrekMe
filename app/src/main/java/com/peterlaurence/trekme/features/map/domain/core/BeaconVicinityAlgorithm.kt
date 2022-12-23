package com.peterlaurence.trekme.features.map.domain.core

import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import kotlinx.coroutines.*

class BeaconVicinityAlgorithm {
    private val beaconsMetadata = mutableMapOf<Beacon, BeaconMetadata>()

    /**
     * For a given [location], compute the distance with each beacon.
     * @return The list of beacon for which the location is the vicinity.
     */
    suspend fun processLocation(location: Location, beacons: List<Beacon>): List<Beacon> = coroutineScope {
        val alerts = mutableListOf<Beacon>()
        for (beacon in beacons) {
            launch {
                val isInside = location.isInside(beacon)
                val shouldAlert = update(beacon, isInside)
                if (shouldAlert) {
                    alerts.add(beacon)
                }
            }
        }
        alerts
    }

    private suspend fun Location.isInside(beacon: Beacon): Boolean = withContext(Dispatchers.Default) {
        distanceApprox(latitude, longitude, beacon.lat, beacon.lon) < beacon.radius
    }

    /**
     * @return Whether an alert should be fired for that beacon.
     */
    private fun update(beacon: Beacon, isInside: Boolean): Boolean {
        val metadata = beaconsMetadata.getOrPut(beacon) { BeaconMetadata() }
        return if (isInside) {
            if (!metadata.isAlerted) {
                metadata.isAlerted = true
                metadata.outsideCount = 0
                true
            } else false
        } else {
            metadata.outsideCount = (metadata.outsideCount + 1).coerceAtMost(10)
            if (metadata.outsideCount == 10 && metadata.isAlerted) {
                metadata.isAlerted = false
            }
            false
        }
    }
}

private data class BeaconMetadata(var isAlerted: Boolean = false, var outsideCount: Int = 0)
