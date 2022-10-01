package com.peterlaurence.trekme.core.map.domain.models

sealed interface MapDownloadResult {
    data class Success(val map: Map): MapDownloadResult
    data class Error(val event: MapDownloadEvent): MapDownloadResult
}