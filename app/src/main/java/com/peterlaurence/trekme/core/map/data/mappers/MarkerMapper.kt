package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.data.models.MarkerGson
import com.peterlaurence.trekme.core.map.data.models.MarkerKtx

/**
 * This is tied to the legacy route format, deprecated in Sept 2021.
 */
fun MarkerGson.Marker.toDomain(): Marker {
    return Marker(
        lat = lat,lon =  lon, name = name, elevation = elevation, comment = comment ?: ""
    )
}

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