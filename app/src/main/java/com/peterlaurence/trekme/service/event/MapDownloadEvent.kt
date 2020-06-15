package com.peterlaurence.trekme.service.event;

sealed class MapDownloadEvent
data class MapDownloadPendingEvent(var progress: Double = 100.0): MapDownloadEvent()
data class MapDownloadFinishedEvent(val mapId: Int): MapDownloadEvent()
object MapDownloadStorageErrorEvent: MapDownloadEvent()

