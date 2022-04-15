package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

interface MapSizeComputeDao {
    suspend fun computeMapSize(map: Map): Result<Long>
}