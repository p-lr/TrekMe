package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LandmarkKtx(
    val id: String? = null,  // The id was introduced on 2022/12, existing data may not have an id.
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val comment: String = ""
)

@Serializable
data class LandmarkListKtx(val landmarks: List<LandmarkKtx>)