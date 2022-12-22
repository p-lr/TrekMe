package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
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
