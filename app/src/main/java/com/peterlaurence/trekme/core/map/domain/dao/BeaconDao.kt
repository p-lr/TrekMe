package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

/**
 * Beacons are lazily loaded.
 */
interface BeaconDao {
    /**
     * Attempts to fetch and set the beacons for the map.
     * Returns the success status.
     */
    suspend fun getBeaconsForMap(map: Map): Boolean

    suspend fun saveBeacons(map: Map)
}