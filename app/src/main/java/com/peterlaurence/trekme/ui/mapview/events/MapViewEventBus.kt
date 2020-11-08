package com.peterlaurence.trekme.ui.mapview.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapViewEventBus {
    private val _trackNameChangeSignal = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackNameChangeSignal = _trackNameChangeSignal.asSharedFlow()

    fun postTrackNameChange() = _trackNameChangeSignal.tryEmit(Unit)

    /**********************************************************************************************/

    private val _trackVisibilityChangedSignal = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackVisibilityChangedSignal = _trackVisibilityChangedSignal.asSharedFlow()

    fun postTrackVisibilityChange() = _trackVisibilityChangedSignal.tryEmit(Unit)
}