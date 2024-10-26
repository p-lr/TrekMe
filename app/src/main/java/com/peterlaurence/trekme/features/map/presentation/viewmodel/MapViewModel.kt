package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.interactors.HasOneExtendedOfferInteractor
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.ElevationFixInteractor
import com.peterlaurence.trekme.core.map.domain.models.ErrorIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.ErrorWmtsLicense
import com.peterlaurence.trekme.core.map.domain.models.FreeLicense
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapUpdateFinished
import com.peterlaurence.trekme.core.map.domain.models.ValidIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.ValidWmtsLicense
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.orientation.model.OrientationSource
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.BeaconInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.LandmarkInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MapLicenseInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MarkerInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceState
import com.peterlaurence.trekme.features.map.domain.repository.TrackFollowRepository
import com.peterlaurence.trekme.features.map.presentation.events.BeaconEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ExcursionWaypointEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.ItineraryEvent
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.events.MarkerEditEvent
import com.peterlaurence.trekme.features.map.presentation.events.PlaceableEvent
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.BeaconLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.DistanceLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.DistanceLineState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.ExcursionWaypointLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.LandmarkLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.LandmarkLinesState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.LiveRouteLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.LocationOrientationLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.MarkerLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.RouteLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.ScaleIndicatorLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.ScaleIndicatorState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.TrackFollowLayer
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.ZoomIndicatorLayer
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.util.map as mapStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.maxScale
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onMarkerLongPress
import ovh.plrapps.mapcompose.api.reloadTiles
import ovh.plrapps.mapcompose.api.rotateTo
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    locationSource: LocationSource,
    orientationSource: OrientationSource,
    mapInteractor: MapInteractor,
    markerInteractor: MarkerInteractor,
    landmarkInteractor: LandmarkInteractor,
    beaconInteractor: BeaconInteractor,
    routeInteractor: RouteInteractor,
    excursionInteractor: ExcursionInteractor,
    mapExcursionInteractor: MapExcursionInteractor,
    private val trackFollowRepository: TrackFollowRepository,
    private val mapComposeTileStreamProviderInteractor: MapComposeTileStreamProviderInteractor,
    val settings: Settings,
    private val mapFeatureEvents: MapFeatureEvents,
    gpxRecordEvents: GpxRecordEvents,
    appEventBus: AppEventBus,
    private val mapLicenseInteractor: MapLicenseInteractor,
    hasOneExtendedOfferInteractor: HasOneExtendedOfferInteractor,
    private val elevationFixInteractor: ElevationFixInteractor,
    private val downloadRepository: DownloadRepository,
    @ApplicationScope processScope: CoroutineScope,
    app: Application
) : ViewModel() {
    private val dataStateFlow = MutableSharedFlow<DataState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow
    val orientationFlow: Flow<Double> = orientationSource.orientationFlow
    val elevationFixFlow: StateFlow<Int> = mapRepository.currentMapFlow.flatMapMerge {
        it?.elevationFix ?: MutableStateFlow(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val purchaseFlow: StateFlow<Boolean> = hasOneExtendedOfferInteractor.getPurchaseFlow(viewModelScope)

    val placeableEvents: Flow<PlaceableEvent> = mapFeatureEvents.placeableEvents
    val startTrackFollowEvent: Flow<Unit> = mapFeatureEvents.startTrackFollowService

    private val _events = Channel<MapEvent>(1)
    val events = _events.receiveAsFlow()

    val locationOrientationLayer: LocationOrientationLayer = LocationOrientationLayer(
        viewModelScope,
        settings,
        dataStateFlow,
        goToBoundingBoxFlow = mapFeatureEvents.goToBoundingBox.mapStateFlow {
            it?.let {
                it.mapId to it.boundingBoxConsumable
            }
        },
        mapInteractor,
        onOutOfBounds = {
            viewModelScope.launch {
                _events.send(MapEvent.CURRENT_LOCATION_OUT_OF_BOUNDS)
            }
        },
        onNoLocation = {
            viewModelScope.launch {
                _events.send(MapEvent.AWAITING_LOCATION)
            }
        }
    )

    val landmarkLayer: LandmarkLayer = LandmarkLayer(viewModelScope, dataStateFlow, landmarkInteractor)

    val markerLayer: MarkerLayer = MarkerLayer(
        viewModelScope,
        dataStateFlow,
        markerInteractor,
        goToMarkerFlow = mapFeatureEvents.goToMarker,
        onMarkerEdit = { marker, mapId ->
            mapFeatureEvents.postPlaceableEvent(MarkerEditEvent(marker, mapId))
        },
        onStartItinerary = { marker ->
            mapFeatureEvents.postPlaceableEvent(ItineraryEvent(marker.lat, marker.lon))
        }
    )

    val excursionWaypointLayer = ExcursionWaypointLayer(
        viewModelScope,
        dataStateFlow,
        excursionInteractor,
        goToExcursionWaypointFlow = mapFeatureEvents.goToExcursionWaypoint,
        onWaypointEdit = { waypoint, excursionId ->
            mapFeatureEvents.postPlaceableEvent(ExcursionWaypointEditEvent(waypoint, excursionId))
        },
        onStartItinerary = {
            mapFeatureEvents.postPlaceableEvent(ItineraryEvent(it.latitude, it.longitude))
        }
    )

    val beaconLayer: BeaconLayer = BeaconLayer(
        viewModelScope,
        dataStateFlow,
        purchaseFlow,
        beaconInteractor,
        onBeaconEdit = { beacon, mapId ->
            mapFeatureEvents.postPlaceableEvent(BeaconEditEvent(beacon, mapId))
        },
        mapFeatureEvents
    )

    val trackFollowLayer = TrackFollowLayer(
        scope = viewModelScope,
        processScope = processScope,
        dataStateFlow = dataStateFlow,
        trackFollowRepository = trackFollowRepository,
        mapFeatureEvents = mapFeatureEvents,
        appContext = app.applicationContext,
        appEventBus = appEventBus,
        onTrackSelected = {
            viewModelScope.launch {
                _events.send(MapEvent.TRACK_TO_FOLLOW_SELECTED)
            }
        }
    )

    val distanceLayer = DistanceLayer(viewModelScope, dataStateFlow.map { it.mapState })

    private val scaleIndicatorLayer = ScaleIndicatorLayer(
        viewModelScope,
        settings.getShowScaleIndicator(),
        settings.getMeasurementSystem(),
        dataStateFlow,
        mapInteractor
    )

    private val zoomIndicatorLayer = ZoomIndicatorLayer(
        viewModelScope,
        settings.getShowZoomIndicator(),
        dataStateFlow,
    )

    val routeLayer = RouteLayer(
        scope = viewModelScope,
        dataStateFlow = dataStateFlow,
        goToRouteFlow = mapFeatureEvents.goToRoute,
        goToExcursionFlow = mapFeatureEvents.goToExcursion,
        routeInteractor = routeInteractor,
        excursionInteractor = excursionInteractor,
        mapExcursionInteractor = mapExcursionInteractor,
        onRouteClick = { route, mapState, map, excursionData ->
            val handled = trackFollowLayer.handleOnPathClick(route.id, mapState, map)
            if (!handled) {
                viewModelScope.launch {
                    _events.send(MapEvent.SHOW_TRACK_BOTTOM_SHEET)
                }
            }
        },
    )

    val liveRouteLayer = LiveRouteLayer(dataStateFlow, routeInteractor, gpxRecordEvents)

    init {
        mapRepository.currentMapFlow.map {
            if (it != null) {
                onMapChange(it)
            }
        }.launchIn(viewModelScope)

        /* Some other components depend on the last scale */
        viewModelScope.launch {
            dataStateFlow.collectLatest {
                snapshotFlow {
                    it.mapState.scale
                }.collect {
                    mapFeatureEvents.postScale(it)
                }
            }
        }

        settings.getMaxScale().combine(dataStateFlow) { maxScale, dataState ->
            dataState.mapState.maxScale = maxScale
        }.launchIn(viewModelScope)

        /* */
        viewModelScope.launch {
            downloadRepository.downloadEvent.collect { event ->
                if (event is MapUpdateFinished) {
                    val (map, mapState) = dataStateFlow.firstOrNull() ?: return@collect
                    if (map.id == event.mapId) {
                        mapState.reloadTiles()
                    }
                }
            }
        }
    }

    suspend fun checkMapLicense() = coroutineScope {
        val map = mapRepository.getCurrentMap() ?: return@coroutineScope

        mapLicenseInteractor.getMapLicenseFlow(map).collectLatest { mapLicense ->
            when (mapLicense) {
                is FreeLicense, ValidIgnLicense, ValidWmtsLicense -> {
                    /* Reload the map only if we were previously in error state */
                    if (_uiState.value is Error) {
                        onMapChange(map)
                    }
                }

                is ErrorIgnLicense -> {
                    _uiState.value = Error.IgnLicenseError
                }

                is ErrorWmtsLicense -> {
                    _uiState.value = Error.WmtsLicenseError
                }
            }
        }
    }

    fun initiateTrackFollow() = viewModelScope.launch {
        if (trackFollowRepository.serviceState.value is TrackFollowServiceState.Started) {
            _events.send(MapEvent.TRACK_TO_FOLLOW_ALREADY_RUNNING)
        } else {
            trackFollowLayer.start()
        }
    }

    fun toggleShowOrientation() = viewModelScope.launch {
        settings.toggleOrientationVisibility()
    }

    fun toggleSpeed() = viewModelScope.launch {
        settings.toggleSpeedVisibility()
    }

    fun toggleShowGpsData() = viewModelScope.launch {
        settings.toggleGpsDataVisibility()
    }

    fun alignToNorth() = viewModelScope.launch {
        dataStateFlow.first().mapState.rotateTo(0f)
    }

    fun onElevationFixUpdate(fix: Int) = viewModelScope.launch {
        elevationFixInteractor.setElevationFix(dataStateFlow.first().map, fix)
    }

    fun isShowingDistanceFlow(): StateFlow<Boolean> = distanceLayer.isVisible
    fun isShowingDistanceOnTrackFlow(): StateFlow<Boolean> = routeLayer.isShowingDistanceOnTrack
    fun isShowingSpeedFlow(): Flow<Boolean> = settings.getSpeedVisibility()
    fun orientationVisibilityFlow(): Flow<Boolean> = settings.getOrientationVisibility()
    fun isLockedOnPosition(): State<Boolean> = locationOrientationLayer.isLockedOnPosition
    fun isShowingGpsDataFlow(): Flow<Boolean> = settings.getGpsDataVisibility()

    /* region map configuration */
    private suspend fun onMapChange(map: Map) {
        /* Shutdown the previous map state, if any */
        dataStateFlow.replayCache.firstOrNull()?.mapState?.shutdown()

        /* For instance, MapCompose only supports levels of uniform tile size (and squared) */
        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            _uiState.value = Error.EmptyMap
            return
        }

        val tileStreamProvider = mapComposeTileStreamProviderInteractor.makeTileStreamProvider(map)

        val magnifyingFactor = settings.getMagnifyingFactor().first()

        val mapState = MapState(
            map.levelList.size,
            map.widthPx,
            map.heightPx,
            tileSize
        ) {
            magnifyingFactor(magnifyingFactor)
            highFidelityColors(false)
        }.apply {
            addLayer(tileStreamProvider)
        }

        /* region Configuration */
        val markerHitHandler = l@{ id: String, x: Double, y: Double ->
            val landmarkHandled = landmarkLayer.onMarkerTap(mapState, map.id, id, x, y)
            if (landmarkHandled) return@l

            val markerHandled = markerLayer.onMarkerTap(mapState, map.id, id, x, y)
            if (markerHandled) return@l

            val excursionWptHandled = excursionWaypointLayer.onMarkerTap(mapState, map.id, id, x, y)
            if (excursionWptHandled) return@l

            beaconLayer.onMarkerTap(mapState, map.id, id, x, y)
        }

        mapState.onMarkerClick { id, x, y ->
            markerHitHandler(id, x, y)
        }

        // Do the same thing as click for long-press
        mapState.onMarkerLongPress { id, x, y ->
            markerHitHandler(id, x, y)
        }
        /* endregion */

        dataStateFlow.tryEmit(DataState(map, mapState))
        val landmarkLinesState = LandmarkLinesState(mapState, map)
        val distanceLineState = DistanceLineState(mapState, map)
        val mapUiState = MapUiState(
            mapState = mapState,
            landmarkLinesState = landmarkLinesState,
            distanceLineState = distanceLineState,
            scaleIndicatorState = scaleIndicatorLayer.state,
            zoomIndicatorState = zoomIndicatorLayer.zoom,
            mapNameFlow = map.name
        )
        _uiState.value = mapUiState
    }
    /* endregion */

    interface MarkerTapListener {
        fun onMarkerTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double): Boolean
    }
}

/**
 * When the [Map] changes, the [MapState] also changes. A [DataState] guarantees this consistency,
 * as opposed to combining separates flows of [Map] and [MapState], which would produce ephemeral
 * inconsistent combinations.
 */
data class DataState(val map: Map, val mapState: MapState)

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val landmarkLinesState: LandmarkLinesState,
    val distanceLineState: DistanceLineState,
    val scaleIndicatorState: ScaleIndicatorState,
    val zoomIndicatorState: StateFlow<Double?>,
    val mapNameFlow: StateFlow<String>
) : UiState

object Loading : UiState
enum class Error : UiState {
    IgnLicenseError, WmtsLicenseError, EmptyMap
}

enum class MapEvent {
    CURRENT_LOCATION_OUT_OF_BOUNDS,
    AWAITING_LOCATION,
    TRACK_TO_FOLLOW_SELECTED,
    TRACK_TO_FOLLOW_ALREADY_RUNNING,
    SHOW_TRACK_BOTTOM_SHEET
}
