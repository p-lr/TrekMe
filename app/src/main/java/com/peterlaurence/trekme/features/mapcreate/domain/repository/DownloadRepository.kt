package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.MapDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import com.peterlaurence.trekme.core.map.domain.models.NewDownloadSpec
import com.peterlaurence.trekme.core.map.domain.models.UpdateSpec
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.UUID

class DownloadRepository {
    private val _status = MutableStateFlow<Status>(Stopped)
    val status = _status.asStateFlow()

    private var downloadSpec: MapDownloadSpec? = null

    private val _downloadEvent: MutableSharedFlow<MapDownloadEvent> = MutableSharedFlow(0, 1, BufferOverflow.DROP_OLDEST)
    val downloadEvent: SharedFlow<MapDownloadEvent> = _downloadEvent.asSharedFlow()

    fun setStatus(status: Status) {
        _status.value = status
    }

    fun isStarted() = when (status.value) {
        is DownloadingNewMap, is UpdatingMap -> true
        Stopped -> false
    }

    /* Should only be invoked from the main thread */
    fun postMapDownloadSpec(spec: MapDownloadSpec) {
        downloadSpec = spec
    }

    /* Should only be invoked from the main thread */
    fun getMapDownloadSpec(): MapDownloadSpec? = downloadSpec

    fun postDownloadEvent(event: MapDownloadEvent) = _downloadEvent.tryEmit(event)

    sealed interface Status
    data class DownloadingNewMap(val downloadSpec: NewDownloadSpec, val mapId: UUID): Status
    data class UpdatingMap(val updateSpec: UpdateSpec): Status
    data object Stopped: Status
}