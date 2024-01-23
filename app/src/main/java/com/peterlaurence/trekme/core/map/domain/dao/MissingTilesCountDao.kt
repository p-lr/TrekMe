package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface MissingTilesCountDao {
    suspend fun setMissingTilesCount(map: Map, count: Long): Boolean
    suspend fun loadMissingTilesCount(map: Map): Long?
}