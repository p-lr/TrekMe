package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

/**
 * Map markers are lazily loaded.
 */
interface GetMarkersForMapDao {
    /**
     * Attempts to fetch and set the markers for the map.
     * Returns the success status.
     */
    suspend fun getMarkersForMap(map: Map): Boolean
}