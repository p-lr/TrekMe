package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider

interface MapDownloadDao {
    suspend fun processRequest(
        request: DownloadMapRequest,
        tileStreamProvider: TileStreamProvider,
        onProgress: (Int) -> Unit
    ): MapDownloadResult
}