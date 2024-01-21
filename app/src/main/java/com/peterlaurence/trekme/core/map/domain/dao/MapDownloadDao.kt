package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.MapDownloadResult
import com.peterlaurence.trekme.core.map.domain.models.NewDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.RepairSpec
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider

interface MapDownloadDao {
    suspend fun processNewDownloadSpec(
        spec: NewDownloadSpec,
        tileStreamProvider: TileStreamProvider,
        onProgress: (Int) -> Unit
    ): MapDownloadResult

    suspend fun processRepairSpec(
        spec: RepairSpec,
        tileStreamProvider: TileStreamProvider,
        onProgress: (Int) -> Unit
    ): MapDownloadResult
}