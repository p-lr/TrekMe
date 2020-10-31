package com.peterlaurence.trekme.core.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Application-wide event-bus.
 *
 * @author P.Laurence on 31/10/2020
 */
class AppEventBus {
    private val _genericMessageEvents = MutableSharedFlow<GenericMessage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val genericMessageEvents = _genericMessageEvents.asSharedFlow()

    fun produceGenericMessage(msg: GenericMessage) {
        _genericMessageEvents.tryEmit(msg)
    }
}