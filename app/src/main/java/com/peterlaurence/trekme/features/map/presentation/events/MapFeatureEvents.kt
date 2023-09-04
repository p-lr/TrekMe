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
    /* region placeable */
    private val _placeableEvents = MutableSharedFlow<PlaceableEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val placeableEvents = _placeableEvents.asSharedFlow()

    fun postPlaceableEvent(event: PlaceableEvent) = _placeableEvents.tryEmit(event)

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

sealed interface PlaceableEvent
data class MarkerEditEvent(val marker: Marker, val mapId: UUID): PlaceableEvent
data class ExcursionWaypointEditEvent(val waypoint: ExcursionWaypoint, val excursionId: String): PlaceableEvent
data class BeaconEditEvent(val beacon: Beacon, val mapId: UUID): PlaceableEvent
data class ItineraryEvent(val latitude: Double, val longitude: Double): PlaceableEvent
