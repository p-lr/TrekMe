package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface MapUpdateDataDao {
    suspend fun setNewDownloadData(map: Map, missingTilesCount: Long)
    suspend fun setRepairData(map: Map, missingTilesCount: Long, date: Long)
    suspend fun setUpdateData(map: Map, missingTilesCount: Long, date: Long)
    suspend fun loadMapUpdateData(map: Map)
}