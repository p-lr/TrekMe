package com.peterlaurence.trekme.service.event;

import java.util.*

sealed class MapDownloadEvent
data class MapDownloadPending(var progress: Double = 100.0): MapDownloadEvent()
data class MapDownloadFinished(val mapId: UUID): MapDownloadEvent()
object MapDownloadStorageError: MapDownloadEvent()
object MapDownloadAlreadyRunning: MapDownloadEvent()

