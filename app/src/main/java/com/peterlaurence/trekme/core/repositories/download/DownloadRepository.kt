package com.peterlaurence.trekme.core.repositories.download

import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class DownloadRepository {
    private var downloadRequest: DownloadMapRequest? = null

    private val _downloadEvent: MutableSharedFlow<MapDownloadEvent> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
    val downloadEvent: SharedFlow<MapDownloadEvent> = _downloadEvent.asSharedFlow()

    /* Should only be invoked from the main thread */
    fun postDownloadMapRequest(request: DownloadMapRequest) {
        downloadRequest = request
    }

    /* Should only be invoked from the main thread */
    fun getDownloadMapRequest(): DownloadMapRequest? = downloadRequest

    fun postDownloadEvent(event: MapDownloadEvent) = _downloadEvent.tryEmit(event)
}