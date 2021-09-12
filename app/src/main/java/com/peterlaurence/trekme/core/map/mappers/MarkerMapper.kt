package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.entity.MarkerGson
import com.peterlaurence.trekme.core.map.entity.MarkerKtx

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

fun Marker.toMarkerKtx(): MarkerKtx {
    return MarkerKtx(
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        proj_x = this.proj_x,
        proj_y = this.proj_y,
        comment = this.comment
    )
}

fun MarkerKtx.toMarker(): Marker {
    return Marker(
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        proj_x = this.proj_x,
        proj_y = this.proj_y,
        comment = this.comment
    )
}