package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.data.MarkerGson
import com.peterlaurence.trekme.core.map.data.MarkerKtx

fun Marker.toEntity(): MarkerGson.Marker {
    val m = this
    return MarkerGson.Marker().apply {
        lat = m.lat
        lon = m.lon
        name = m.name
        elevation = m.elevation
        comment = m.comment
    }
}

fun MarkerGson.Marker.toDomain(): Marker {
    return Marker(lat, lon, name, elevation, comment)
}

fun Marker.toMarkerKtx(): MarkerKtx {
    return MarkerKtx(
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        comment = this.comment
    )
}

fun MarkerKtx.toMarker(): Marker {
    return Marker(
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        comment = this.comment
    )
}