package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.Serializable

@Serializable
data class MarkerKtx(
    val id: String? = null,  // The id was introduced on 2022/12, existing data may not have an id.
    val lat: Double,
    val lon: Double,
    val name: String? = null,
    val elevation: Double? = null,
    val comment: String? = null,
    val color: String? = null  // Color introduced on 2024/02, existing data may not have it
)

@Serializable
data class MarkerListKtx(val markers: List<MarkerKtx>)