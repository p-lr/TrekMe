package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface UpdateMapSizeInBytesDao {
    suspend fun updateMapSize(map: Map): Result<Long>
}