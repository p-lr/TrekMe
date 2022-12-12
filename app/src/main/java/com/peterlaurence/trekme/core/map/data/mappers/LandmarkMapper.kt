package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.data.models.LandmarkKtx
import com.peterlaurence.trekme.core.map.domain.models.Landmark

fun Landmark.toLandmarkKtx(): LandmarkKtx {
    return LandmarkKtx(id = id, lat = lat, lon = lon, name = name, comment = comment)
}

fun LandmarkKtx.toDomain(): Landmark {
    return if (id != null) {
        Landmark(id = id, name = name, lat = lat, lon = lon, comment = comment)
    } else {
        Landmark(name = name, lat = lat, lon = lon, comment = comment)
    }
}