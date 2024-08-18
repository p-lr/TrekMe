package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadResult
import com.peterlaurence.trekme.core.map.domain.models.NewDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.map.domain.models.UpdateSpec

interface MapDownloadDao {
    suspend fun processNewDownloadSpec(
        spec: NewDownloadSpec,
        tileStreamProvider: TileStreamProvider,
        onMapCreated: suspend (Map) -> Unit,
        onProgress: (Int) -> Unit
    ): MapDownloadResult

    suspend fun processUpdateSpec(
        spec: UpdateSpec,
        tileStreamProvider: TileStreamProvider,
        onProgress: (Int) -> Unit
    ): MapDownloadResult
}