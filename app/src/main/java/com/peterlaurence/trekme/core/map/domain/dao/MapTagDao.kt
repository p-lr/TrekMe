package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.TileTag

interface MapTagDao {
    suspend fun getTag(map: Map): TileTag?
}

