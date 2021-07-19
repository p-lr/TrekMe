package com.peterlaurence.trekme.ui.gpspro.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GpsProEvents {
    private val _requestShowGpsProFragment = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestShowGpsProFragment = _requestShowGpsProFragment.asSharedFlow()

    fun requestShowGpsProFragment() = _requestShowGpsProFragment.tryEmit(Unit)

    /**********************************************************************************************/

    private val _nmeaSentencesFlow = MutableSharedFlow<String>(extraBufferCapacity = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val nmeaSentencesFlow = _nmeaSentencesFlow.asSharedFlow()

    fun postNmeaSentence(sentence: String) = _nmeaSentencesFlow.tryEmit(sentence)
}