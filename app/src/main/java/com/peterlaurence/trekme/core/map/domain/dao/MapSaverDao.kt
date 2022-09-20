package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map

interface MapSaverDao {
    suspend fun save(map: Map)
}