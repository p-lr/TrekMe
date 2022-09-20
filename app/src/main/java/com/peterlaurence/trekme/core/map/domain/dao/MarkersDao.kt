package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

/**
 * Map markers are lazily loaded.
 */
interface MarkersDao {
    /**
     * Attempts to fetch and set the markers for the map.
     * Returns the success status.
     */
    suspend fun getMarkersForMap(map: Map): Boolean

    suspend fun saveMarkers(map: Map)
}