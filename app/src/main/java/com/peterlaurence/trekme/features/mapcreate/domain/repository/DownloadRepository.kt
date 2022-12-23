package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class DownloadRepository {
    private val _started = MutableStateFlow(false)
    val started = _started.asStateFlow()

    private var downloadRequest: DownloadMapRequest? = null

    private val _downloadEvent: MutableSharedFlow<MapDownloadEvent> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
    val downloadEvent: SharedFlow<MapDownloadEvent> = _downloadEvent.asSharedFlow()

    fun setDownloadInProgress(started: Boolean) {
        _started.value = started
    }

    /* Should only be invoked from the main thread */
    fun postDownloadMapRequest(request: DownloadMapRequest) {
        downloadRequest = request
    }

    /* Should only be invoked from the main thread */
    fun getDownloadMapRequest(): DownloadMapRequest? = downloadRequest

    fun postDownloadEvent(event: MapDownloadEvent) = _downloadEvent.tryEmit(event)
}