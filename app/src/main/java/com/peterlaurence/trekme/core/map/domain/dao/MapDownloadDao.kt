package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.MapDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider

interface MapDownloadDao {
    suspend fun processDownloadSpec(
        spec: MapDownloadSpec,
        tileStreamProvider: TileStreamProvider,
        onProgress: (Int) -> Unit
    ): MapDownloadResult
}