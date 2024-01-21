package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapPropertiesKtx(
    val elevationFix: Int = 0,
    @SerialName("size_in_bytes")
    val sizeInBytes: Long = 0
)