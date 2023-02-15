package com.peterlaurence.trekme.features.mapcreate.presentation.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapCreateEventBus {
    private val _layerSelectEvent = MutableSharedFlow<String>(0, 1, BufferOverflow.DROP_OLDEST)
    val layerSelectEvent = _layerSelectEvent.asSharedFlow()

    fun postLayerSelectEvent(layer: String) = _layerSelectEvent.tryEmit(layer)

    /**********************************************************************************************/
}