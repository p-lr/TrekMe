package com.peterlaurence.trekme.features.map.presentation.events

import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.util.*

class MapFeatureEvents {
    /* region markers */
    private val _navigateToMarkerEdit = MutableSharedFlow<MarkerEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToMarkerEdit = _navigateToMarkerEdit.asSharedFlow()
    fun postMarkerEditEvent(marker: Marker, mapId: UUID, markerId: String) = _navigateToMarkerEdit.tryEmit(
        MarkerEditEvent(marker, mapId, markerId)
    )
    data class MarkerEditEvent(val marker: Marker, val mapId: UUID, val markerId: String)

    // TODO: once markers are refactored to be based on a flow (like beacons), we no longer need that event
    private val _markerMoved = MutableSharedFlow<MarkerMovedEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val markerMoved = _markerMoved.asSharedFlow()
    fun postMarkerMovedEvent(marker: Marker, mapId: UUID, markerId: String) = _markerMoved.tryEmit(
        MarkerMovedEvent(marker, mapId, markerId)
    )
    data class MarkerMovedEvent(val marker: Marker, val mapId: UUID, val markerId: String)
    /* endregion */

    /* region beacon */
    private val _navigateToBeaconEdit = MutableSharedFlow<BeaconEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToBeaconEdit = _navigateToBeaconEdit.asSharedFlow()
    fun postBeaconEditEvent(beacon: Beacon, mapId: UUID) = _navigateToBeaconEdit.tryEmit(
        BeaconEditEvent(beacon, mapId)
    )

    data class BeaconEditEvent(val beacon: Beacon, val mapId: UUID)
    /* endregion */

    /* region routes */
    private val _goToRoute = MutableSharedFlow<Route>(0, 1, BufferOverflow.DROP_OLDEST)
    val goToRoute = _goToRoute.asSharedFlow()

    fun postGoToRoute(route: Route) = _goToRoute.tryEmit(route)
    /* endregion */

    /* region public properties */
    private val _mapScaleFlow = MutableStateFlow<Float?>(null)
    val mapScaleFlow: StateFlow<Float?> = _mapScaleFlow.asStateFlow()

    fun postScale(scale: Float) {
        _mapScaleFlow.value = scale
    }
    /* endregion */
}