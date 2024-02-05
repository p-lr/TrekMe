package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.data.models.MarkerKtx

fun Marker.toMarkerKtx(): MarkerKtx {
    return MarkerKtx(
        id = this.id,
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        comment = this.comment,
        color = color
    )
}

fun MarkerKtx.toDomain(): Marker {
    return Marker.make(
        id = id,
        lat = this.lat,
        lon = this.lon,
        name = this.name,
        elevation = this.elevation,
        time = null,
        comment = this.comment,
        color = this.color
    )
}