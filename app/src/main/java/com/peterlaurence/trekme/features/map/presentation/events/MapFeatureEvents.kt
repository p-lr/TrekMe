package com.peterlaurence.trekme.features.map.presentation.events

import com.peterlaurence.trekme.core.map.domain.Marker
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class MapFeatureEvents {
    /* region markers */
    private val _navigateToMarkerEdit = MutableSharedFlow<MarkerEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToMarkerEdit = _navigateToMarkerEdit.asSharedFlow()
    fun postMarkerEditEvent(marker: Marker, mapId: Int, markerId: String) = _navigateToMarkerEdit.tryEmit(
        MarkerEditEvent(marker, mapId, markerId)
    )
    data class MarkerEditEvent(val marker: Marker, val mapId: Int, val markerId: String)


    private val _markerMoved = MutableSharedFlow<MarkerMovedEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val markerMoved = _markerMoved.asSharedFlow()
    fun postMarkerMovedEvent(marker: Marker, mapId: Int, markerId: String) = _markerMoved.tryEmit(
        MarkerMovedEvent(marker, mapId, markerId)
    )
    data class MarkerMovedEvent(val marker: Marker, val mapId: Int, val markerId: String)
    /* endregion */

    /* region public properties */
    private val _mapScaleFlow = MutableStateFlow<Float?>(null)
    val mapScaleFlow: StateFlow<Float?> = _mapScaleFlow.asStateFlow()

    fun postScale(scale: Float) {
        _mapScaleFlow.value = scale
    }
    /* endregion */
}