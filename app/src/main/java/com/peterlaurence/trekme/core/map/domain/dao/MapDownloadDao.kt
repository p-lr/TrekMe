package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent

interface MapDownloadDao {
    suspend fun processRequest(request: DownloadMapRequest, onProgress: (Int) -> Unit): MapDownloadResult

    sealed interface MapDownloadResult {
        data class Success(val map: Map): MapDownloadResult
        data class Error(val event: MapDownloadEvent): MapDownloadResult
    }
}