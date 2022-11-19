package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.data.models.BeaconKtx
import com.peterlaurence.trekme.core.map.domain.models.Beacon

fun Beacon.toBeaconKtx(): BeaconKtx {
    return BeaconKtx(id = id, lat = lat, lon = lon, radius = radius, name = name, comment = comment)
}

fun BeaconKtx.toDomain(): Beacon {
    return Beacon(id = id, lat = lat, lon = lon, radius = radius, name = name, comment = comment ?: "")
}
