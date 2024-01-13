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
        comment = this.comment
    )
}

fun MarkerKtx.toDomain(): Marker {
    return if (id != null) {
        Marker(
            id = id,
            lat = this.lat,
            lon = this.lon,
            name = this.name ?: "",
            elevation = this.elevation,
            comment = this.comment ?: ""
        )
    } else {
        Marker(
            lat = this.lat,
            lon = this.lon,
            name = this.name ?: "",
            elevation = this.elevation,
            comment = this.comment ?: ""
        )
    }
}