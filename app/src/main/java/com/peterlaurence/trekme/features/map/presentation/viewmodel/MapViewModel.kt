package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.ElevationFixInteractor
import com.peterlaurence.trekme.core.map.domain.models.ErrorIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.FreeLicense
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.ValidIgnLicense
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.orientation.model.OrientationSource
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.*
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.SnackBarController
import com.peterlaurence.trekme.features.map.presentation.viewmodel.layers.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*
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
    private val mapComposeTileStreamProviderInteractor: MapComposeTileStreamProviderInteractor,
    val settings: Settings,
    private val mapFeatureEvents: MapFeatureEvents,
    gpxRecordEvents: GpxRecordEvents,
    private val appEventBus: AppEventBus,
    private val mapLicenseInteractor: MapLicenseInteractor,
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val elevationFixInteractor: ElevationFixInteractor
) : ViewModel() {
    private val dataStateFlow = MutableSharedFlow<DataState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow
    val orientationFlow: Flow<Double> = orientationSource.orientationFlow
    val elevationFixFlow: StateFlow<Int> = mapRepository.currentMapFlow.flatMapMerge {
        it?.elevationFix ?: MutableStateFlow(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val purchaseFlow: StateFlow<PurchaseState> = extendedOfferStateOwner.purchaseFlow

    val markerEditEvent: Flow<MapFeatureEvents.MarkerEditEvent> = mapFeatureEvents.navigateToMarkerEdit
    val beaconEditEvent: Flow<MapFeatureEvents.BeaconEditEvent> = mapFeatureEvents.navigateToBeaconEdit

    val locationOrientationLayer: LocationOrientationLayer = LocationOrientationLayer(
        viewModelScope,
        settings,
        dataStateFlow,
        mapInteractor,
        onOutOfBounds = {
            snackBarController.showSnackBar(SnackBarEvent.CURRENT_LOCATION_OUT_OF_BOUNDS)
        }
    )

    val landmarkLayer: LandmarkLayer = LandmarkLayer(viewModelScope, dataStateFlow, landmarkInteractor)

    val markerLayer: MarkerLayer = MarkerLayer(
        viewModelScope,
        dataStateFlow,
        markerInteractor,
        onMarkerEdit = { marker, mapId ->
            mapFeatureEvents.postMarkerEditEvent(marker, mapId)
        }
    )

    val beaconLayer: BeaconLayer = BeaconLayer(
        viewModelScope,
        dataStateFlow,
        purchaseFlow,
        beaconInteractor,
        onBeaconEdit = { beacon, mapId ->
            mapFeatureEvents.postBeaconEditEvent(beacon, mapId)
        },
        mapFeatureEvents
    )

    val distanceLayer = DistanceLayer(viewModelScope, dataStateFlow.map { it.mapState })

    private val scaleIndicatorLayer = ScaleIndicatorLayer(
        viewModelScope,
        settings.getShowScaleIndicator(),
        settings.getMeasurementSystem(),
        dataStateFlow,
        mapInteractor
    )

    val routeLayer = RouteLayer(
        viewModelScope,
        dataStateFlow,
        mapFeatureEvents.goToRoute,
        mapFeatureEvents.goToExcursion,
        routeInteractor,
        excursionInteractor
    )

    val snackBarController = SnackBarController()

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

        LiveRouteLayer(viewModelScope, dataStateFlow, routeInteractor, gpxRecordEvents)
    }

    /* region events */
    fun onMainMenuClick() {
        appEventBus.openDrawer()
    }

    fun onShopClick() {
        appEventBus.navigateToShop()
    }
    /* endregion */

    suspend fun checkMapLicense() = coroutineScope {
        val map = mapRepository.getCurrentMap() ?: return@coroutineScope

        mapLicenseInteractor.getMapLicenseFlow(map).collectLatest { mapLicense ->
            when (mapLicense) {
                is FreeLicense, ValidIgnLicense -> {
                    /* Reload the map only if we were previously in error state */
                    if (_uiState.value is Error) {
                        onMapChange(map)
                    }
                }
                is ErrorIgnLicense -> {
                    _uiState.value = Error.LicenseError
                }
            }
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
        mapState.shouldLoopScale = true

        mapState.onMarkerClick { id, x, y ->
            val landmarkHandled = landmarkLayer.onMarkerTap(mapState, map.id, id, x, y)
            if (landmarkHandled) return@onMarkerClick

            val markerHandled = markerLayer.onMarkerTap(mapState, map.id, id, x, y)
            if (markerHandled) return@onMarkerClick

            beaconLayer.onMarkerTap(mapState, map.id, id, x, y)
        }
        /* endregion */

        dataStateFlow.tryEmit(DataState(map, mapState))
        val landmarkLinesState = LandmarkLinesState(mapState, map)
        val distanceLineState = DistanceLineState(mapState, map)
        val mapUiState = MapUiState(
            mapState,
            landmarkLinesState,
            distanceLineState,
            scaleIndicatorLayer.state
        )
        _uiState.value = mapUiState
    }
    /* endregion */

    interface MarkerTapListener {
        fun onMarkerTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double) : Boolean
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
    val scaleIndicatorState: ScaleIndicatorState
) : UiState

object Loading : UiState
enum class Error : UiState {
    LicenseError, EmptyMap
}

enum class SnackBarEvent {
    CURRENT_LOCATION_OUT_OF_BOUNDS
}
