package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.Serializable

@Serializable
data class BeaconKtx(
    val id: String,
    val lat: Double,
    val lon: Double,
    val radius: Float,
    val name: String = "",
    val comment: String? = null
)

@Serializable
data class BeaconListKtx(val beacons: List<BeaconKtx>)