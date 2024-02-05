package com.peterlaurence.trekme.core.map.domain.models

import java.util.*

/**
 * TODO: Instead of using immutable properties, use observable properties like in Route type (beware
 * that this change has deep repercussion on how the MapLayer works).
 */
data class Marker(
    val id: String = UUID.randomUUID().toString(),
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val elevation: Double? = null,
    val time: Long? = null,
    val comment: String = "",
    val color: String = colorMarker, // In the format "#AARRGGBB"
) {
    companion object {
        /* This factory is useful when creating a Marker instance from unknown combination of
         * parameters. */
        fun make(
            id: String?,
            lat: Double,
            lon: Double,
            name: String?,
            elevation: Double?,
            time: Long?,
            comment: String?,
            color: String?, // In the format "#AARRGGBB"
        ): Marker {
            return Marker(
                id = id ?: UUID.randomUUID().toString(),
                lat = lat,
                lon = lon,
                name = name ?: "",
                elevation = elevation,
                time = time,
                comment = comment ?: "",
                color = color ?: colorMarker
            )
        }
    }
}

private const val colorMarker = "#fff16157"