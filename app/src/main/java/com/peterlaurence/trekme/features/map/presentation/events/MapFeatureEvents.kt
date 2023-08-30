package com.peterlaurence.trekme.features.map.presentation.events

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceStopEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*

class MapFeatureEvents {
    /* region markers */
    private val _navigateToMarkerEdit = MutableSharedFlow<MarkerEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToMarkerEdit = _navigateToMarkerEdit.asSharedFlow()
    fun postMarkerEditEvent(marker: Marker, mapId: UUID) = _navigateToMarkerEdit.tryEmit(
        MarkerEditEvent(marker, mapId)
    )
    data class MarkerEditEvent(val marker: Marker, val mapId: UUID)

    /* region excursion waypoints */
    private val _navigateToExcursionWaypointEdit = MutableSharedFlow<ExcursionWaypointEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToExcursionWaypointEdit = _navigateToExcursionWaypointEdit.asSharedFlow()
    fun postExcursionWaypointEditEvent(waypoint: ExcursionWaypoint, excursionId: String) = _navigateToExcursionWaypointEdit.tryEmit(
        ExcursionWaypointEditEvent(waypoint, excursionId)
    )
    data class ExcursionWaypointEditEvent(val waypoint: ExcursionWaypoint, val excursionId: String)

    /* region beacon */
    private val _navigateToBeaconEdit = MutableSharedFlow<BeaconEditEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val navigateToBeaconEdit = _navigateToBeaconEdit.asSharedFlow()
    fun postBeaconEditEvent(beacon: Beacon, mapId: UUID) = _navigateToBeaconEdit.tryEmit(
        BeaconEditEvent(beacon, mapId)
    )

    data class BeaconEditEvent(val beacon: Beacon, val mapId: UUID)

    private val _hasBeacons = Channel<Unit>(1)
    val hasBeaconsFlow = _hasBeacons.receiveAsFlow() // This channel-based flow works well with only one collector

    fun postHasBeacons() {
        _hasBeacons.trySend(Unit)
    }
    /* endregion */

    /* region routes */
    private val _goToRoute = MutableSharedFlow<Route>(0, 1, BufferOverflow.DROP_OLDEST)
    val goToRoute = _goToRoute.asSharedFlow()

    fun postGoToRoute(route: Route) = _goToRoute.tryEmit(route)
    /* endregion */

    /* region excursion */
    private val _goToExcursion = MutableSharedFlow<ExcursionRef>(0, 1, BufferOverflow.DROP_OLDEST)
    val goToExcursion = _goToExcursion.asSharedFlow()

    fun postGoToExcursion(ref: ExcursionRef) = _goToExcursion.tryEmit(ref)
    /* endregion */

    /* region public properties */
    private val _mapScaleFlow = MutableStateFlow<Float?>(null)
    val mapScaleFlow: StateFlow<Float?> = _mapScaleFlow.asStateFlow()

    fun postScale(scale: Float) {
        _mapScaleFlow.value = scale
    }
    /* endregion */

    /* region track follow */
    private val _startTrackFollowService = Channel<Unit>(1)
    val startTrackFollowService = _startTrackFollowService.receiveAsFlow()

    fun postStartTrackFollowService() {
        _startTrackFollowService.trySend(Unit)
    }

    private val _trackFollowStopEvent = MutableSharedFlow<TrackFollowServiceStopEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackFollowStopEvent = _trackFollowStopEvent.asSharedFlow()

    fun postTrackFollowStopEvent(event: TrackFollowServiceStopEvent) = _trackFollowStopEvent.tryEmit(event)
    /* endregion */
}