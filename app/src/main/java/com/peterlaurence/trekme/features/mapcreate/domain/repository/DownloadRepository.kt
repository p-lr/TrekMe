package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.MapDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class DownloadRepository {
    private val _started = MutableStateFlow(false)
    val started = _started.asStateFlow()

    private var downloadSpec: MapDownloadSpec? = null

    private val _downloadEvent: MutableSharedFlow<MapDownloadEvent> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
    val downloadEvent: SharedFlow<MapDownloadEvent> = _downloadEvent.asSharedFlow()

    fun setDownloadInProgress(started: Boolean) {
        _started.value = started
    }

    /* Should only be invoked from the main thread */
    fun postMapDownloadSpec(spec: MapDownloadSpec) {
        downloadSpec = spec
    }

    /* Should only be invoked from the main thread */
    fun getMapDownloadSpec(): MapDownloadSpec? = downloadSpec

    fun postDownloadEvent(event: MapDownloadEvent) = _downloadEvent.tryEmit(event)
}