package com.peterlaurence.trekme.features.wifip2p.presentation.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WifiP2pEventBus {
    private val _mapSelectedEvent = MutableSharedFlow<Int>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val mapSelectedEvent = _mapSelectedEvent.asSharedFlow()

    fun setMapSelected(mapId: Int) {
        _mapSelectedEvent.tryEmit(mapId)
    }
}