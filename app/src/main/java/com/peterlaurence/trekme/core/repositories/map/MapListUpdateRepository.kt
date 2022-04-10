package com.peterlaurence.trekme.core.repositories.map

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Now that MapRepository has StateFlow of List<Map>, this repository (below) shouldn't be
// needed anymore.
@Singleton
class MapListUpdateRepository @Inject constructor() {
    private val _mapListUpdateEventFlow =
        MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val mapListUpdateEventFlow = _mapListUpdateEventFlow.asSharedFlow()

    fun notifyMapListUpdate() {
        _mapListUpdateEventFlow.tryEmit(Unit)
    }
}