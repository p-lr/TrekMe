package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadResult

interface MapDownloadDao {
    suspend fun processRequest(request: DownloadMapRequest, onProgress: (Int) -> Unit): MapDownloadResult

}