package com.peterlaurence.trekme.core.georecord.domain.model

import com.peterlaurence.trekme.core.map.domain.models.BoundingBox

/**
 * Container for statistics of a track.
 *
 * @param distance The distance in meters
 * @param elevationUpStack The cumulative elevation up in meters
 * @param elevationDownStack The cumulative elevation down in meters
 * @param elevationMax The highest altitude
 * @param elevationMin The lowest altitude
 * @param durationInSecond The total time in seconds
 * @param avgSpeed The average speed in meters per seconds
 */
data class GeoStatistics(
    val distance: Double, var elevationMax: Double?, var elevationMin: Double?,
    val elevationUpStack: Double, val elevationDownStack: Double,
    val durationInSecond: Long? = null, val avgSpeed: Double? = null,
    val boundingBox: BoundingBox? = null
)

val GeoStatistics.hasElevation
    get() = elevationMin != null && elevationMax != null