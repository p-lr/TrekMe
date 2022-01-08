package com.peterlaurence.trekme.features.map.presentation.events

import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.domain.Route
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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

    /* region tracks */
    private val _trackVisibilityChanged = MutableSharedFlow<TrackVisibilityChangedEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackVisibilityChanged = _trackVisibilityChanged.asSharedFlow()
    fun postTrackVisibilityChanged(mapId: Int, route: Route) = _trackVisibilityChanged.tryEmit(
        TrackVisibilityChangedEvent(mapId, route)
    )
    data class TrackVisibilityChangedEvent(val mapId: Int, val route: Route)
    /* endregion */
}