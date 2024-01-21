package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapRepairKtx(
    @SerialName("missing_tiles_count")
    val missingTilesCount: Long = 0L
)
