package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

interface MapDeleteDao {
    suspend fun deleteMap(map: Map)
}