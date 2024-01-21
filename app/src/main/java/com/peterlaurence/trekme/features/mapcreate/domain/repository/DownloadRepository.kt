package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.MapDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class DownloadRepository {
    private val _status = MutableStateFlow<Status>(Stopped)
    val status = _status.asStateFlow()

    private var downloadSpec: MapDownloadSpec? = null

    private val _downloadEvent: MutableSharedFlow<MapDownloadEvent> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
    val downloadEvent: SharedFlow<MapDownloadEvent> = _downloadEvent.asSharedFlow()

    fun setStatus(status: Status) {
        _status.value = status
    }

    fun isStarted() = _status.value is Started

    /* Should only be invoked from the main thread */
    fun postMapDownloadSpec(spec: MapDownloadSpec) {
        downloadSpec = spec
    }

    /* Should only be invoked from the main thread */
    fun getMapDownloadSpec(): MapDownloadSpec? = downloadSpec

    fun postDownloadEvent(event: MapDownloadEvent) = _downloadEvent.tryEmit(event)

    sealed interface Status
    data class Started(val downloadSpec: MapDownloadSpec): Status
    data object Stopped: Status
}