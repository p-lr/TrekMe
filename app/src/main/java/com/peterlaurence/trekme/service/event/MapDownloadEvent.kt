package com.peterlaurence.trekme.service.event;

sealed class MapDownloadEvent
data class MapDownloadPending(var progress: Double = 100.0): MapDownloadEvent()
data class MapDownloadFinished(val mapId: Int): MapDownloadEvent()
object MapDownloadStorageError: MapDownloadEvent()
object MapDownloadAlreadyRunning: MapDownloadEvent()

