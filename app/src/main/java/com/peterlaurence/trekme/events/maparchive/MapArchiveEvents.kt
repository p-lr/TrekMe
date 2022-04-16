package com.peterlaurence.trekme.events.maparchive

import com.peterlaurence.trekme.core.map.domain.interactors.ArchiveMapInteractor
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class MapArchiveEvents {
    val mapArchiveEventFlow = MutableSharedFlow<ArchiveMapInteractor.ZipEvent>(0, 1, BufferOverflow.DROP_OLDEST)

    fun postEvent(event: ArchiveMapInteractor.ZipEvent) {
        mapArchiveEventFlow.tryEmit(event)
    }
}