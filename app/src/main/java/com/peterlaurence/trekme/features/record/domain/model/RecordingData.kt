package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import java.io.File

/**
 * A [RecordingData] is a wrapper on the [File] and various other data such as the [GeoStatistics]
 * data, the id of the track (if any), and timestamp. The timestamp is used to sort visual elements.
 */
data class RecordingData(
    val file: File, val name: String,
    val statistics: GeoStatistics? = null,
    val routeIds: List<String> = emptyList(),
    val time: Long? = null
)