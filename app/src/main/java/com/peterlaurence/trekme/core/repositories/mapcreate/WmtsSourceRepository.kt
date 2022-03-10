package com.peterlaurence.trekme.core.repositories.mapcreate

import com.peterlaurence.trekme.core.mapsource.WmtsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WmtsSourceRepository {
    private val _wmtsSourceState = MutableStateFlow<WmtsSource?>(null)
    val wmtsSourceState: StateFlow<WmtsSource?> = _wmtsSourceState.asStateFlow()

    fun setMapSource(source: WmtsSource) {
        _wmtsSourceState.tryEmit(source)
    }
}
