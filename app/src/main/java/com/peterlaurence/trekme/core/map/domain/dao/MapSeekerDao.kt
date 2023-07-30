package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import java.io.File

interface MapSeekerDao {

    val status: MapParseStatus

    /**
     * Produces a [Map] from a given [File].
     */
    suspend fun seek(file: File): Result<Map>

    open class MapParseException(message: String) : Exception(message)
}