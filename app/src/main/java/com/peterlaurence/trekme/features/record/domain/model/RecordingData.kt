package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics

/**
 * A [RecordingData] is a wrapper on [GeoStatistics] and various other data such as the ids of the
 * routes (if any), and timestamp. The timestamp is used to sort visual elements.
 */
data class RecordingData(
    val id: String,
    val name: String,
    val statistics: GeoStatistics? = null,
    val routeIds: List<String> = emptyList(),
    val time: Long? = null
)