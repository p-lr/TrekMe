package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource

interface CheckTileStreamProviderDao {
    suspend fun check(wmtsSource: WmtsSource, tileStreamProvider: TileStreamProvider): Boolean
}