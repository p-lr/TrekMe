package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapUpdateKtx(
    @SerialName("missing_tiles_count")
    val missingTilesCount: Long = 0L,
    @SerialName("last_repair_date")
    val lastRepairDate: Long? = null,
    @SerialName("last_update_date")
    val lastUpdateDate: Long? = null
)
