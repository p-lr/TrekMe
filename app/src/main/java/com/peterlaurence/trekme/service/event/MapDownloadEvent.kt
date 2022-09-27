package com.peterlaurence.trekme.service.event;

import java.util.*

sealed class MapDownloadEvent
data class MapDownloadPending(var progress: Int = 100): MapDownloadEvent()
data class MapDownloadFinished(val mapId: UUID): MapDownloadEvent()
object MapDownloadStorageError: MapDownloadEvent()
object MapDownloadAlreadyRunning: MapDownloadEvent()

