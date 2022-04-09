package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

/**
 * Landmarks are lazily loaded.
 */
interface GetLandmarksForMapDao {
    /**
     * Attempts to fetch and set the landmarks for the map.
     * Returns the success status.
     */
    suspend fun getLandmarksForMap(map: Map): Boolean
}