package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.entity.MarkerGson

fun Marker.toEntity(): MarkerGson.Marker {
    val m = this
    return MarkerGson.Marker().apply {
        lat = m.lat
        lon = m.lon
        name = m.name
        elevation = m.elevation
        proj_x = m.proj_x
        proj_y = m.proj_y
        comment = m.comment
    }
}

fun MarkerGson.Marker.toDomain(): Marker {
    return Marker(lat, lon, name, elevation, proj_x, proj_y, comment)
}