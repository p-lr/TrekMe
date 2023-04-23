package com.peterlaurence.trekme.core.wmts.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData

interface TileStreamProviderDao {
    suspend fun newTileStreamProvider(data: MapSourceData): Result<TileStreamProvider>
}