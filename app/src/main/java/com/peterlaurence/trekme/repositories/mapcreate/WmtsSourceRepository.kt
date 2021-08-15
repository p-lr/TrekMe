package com.peterlaurence.trekme.repositories.mapcreate

import com.peterlaurence.trekme.core.mapsource.WmtsSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WmtsSourceRepository {
    private val wmtsSourceState_ = MutableStateFlow<WmtsSource?>(null)
    val wmtsSourceState: StateFlow<WmtsSource?> = wmtsSourceState_.asStateFlow()

    fun setMapSource(source: WmtsSource) {
        wmtsSourceState_.tryEmit(source)
    }

}
