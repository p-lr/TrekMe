package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

interface MapSaverDao {
    suspend fun save(map: Map)
}