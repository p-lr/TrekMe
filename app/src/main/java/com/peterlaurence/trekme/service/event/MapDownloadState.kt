package com.peterlaurence.trekme.service.event;

sealed class MapDownloadState
data class MapDownloadPending(var progress: Double = 100.0): MapDownloadState()
data class MapDownloadFinished(val mapId: Int): MapDownloadState()
object MapDownloadStorageError: MapDownloadState()

