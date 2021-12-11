package com.peterlaurence.trekme.features.map.presentation.events

import com.peterlaurence.trekme.core.map.domain.Marker
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapFeatureEvents {
    /* region markers */
    private val _navigateToMarkerEdit = MutableSharedFlow<MarkerEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToMarkerEdit = _navigateToMarkerEdit.asSharedFlow()
    fun postMarkerEditEvent(marker: Marker, mapId: Int) = _navigateToMarkerEdit.tryEmit(
        MarkerEditEvent(marker, mapId)
    )
    data class MarkerEditEvent(val marker: Marker, val mapId: Int)
    /* endregion */
}