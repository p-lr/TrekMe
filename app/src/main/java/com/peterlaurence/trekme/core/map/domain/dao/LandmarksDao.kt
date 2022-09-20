package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

/**
 * Landmarks are lazily loaded.
 */
interface LandmarksDao {
    /**
     * Attempts to fetch and set the landmarks for the map.
     * Returns the success status.
     */
    suspend fun getLandmarksForMap(map: Map): Boolean

    suspend fun saveLandmarks(map: Map)
}