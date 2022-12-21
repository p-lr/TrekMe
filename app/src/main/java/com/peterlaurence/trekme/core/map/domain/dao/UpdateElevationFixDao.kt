package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface UpdateElevationFixDao {
    suspend fun setElevationFix(map: Map, fix: Int): Boolean
}