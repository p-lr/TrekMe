package com.peterlaurence.trekme.repositories.download

import com.peterlaurence.trekme.service.event.MapDownloadPending
import com.peterlaurence.trekme.service.event.MapDownloadState
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class DownloadRepository {
    /* This event is "sticky" */
    private val _downloadMapRequestEvent = MutableSharedFlow<RequestDownloadMapEvent>(1, 0, BufferOverflow.DROP_OLDEST)
    val downloadMapRequestEvent = _downloadMapRequestEvent.asSharedFlow()

    fun postDownloadMapRequest(request: RequestDownloadMapEvent) = _downloadMapRequestEvent.tryEmit(request)

    /**********************************************************************************************/

    private val _downloadState: MutableStateFlow<MapDownloadState> = MutableStateFlow(MapDownloadPending(0.0))
    val downloadState: StateFlow<MapDownloadState> = _downloadState.asStateFlow()

    fun setDownloadState(state: MapDownloadState) {
        _downloadState.value = state
    }
}