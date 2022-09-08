package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.location.LocationSource
import com.peterlaurence.trekme.core.map.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.interactors.ElevationFixInteractor
import com.peterlaurence.trekme.core.orientation.OrientationSource
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.MapLicenseInteractor
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
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    locationSource: LocationSource,
    orientationSource: OrientationSource,
    mapInteractor: MapInteractor,
    private val mapComposeTileStreamProviderInteractor: MapComposeTileStreamProviderInteractor,
    val settings: Settings,
    private val mapFeatureEvents: MapFeatureEvents,
    gpxRecordEvents: GpxRecordEvents,
    private val appEventBus: AppEventBus,
    private val mapLicenseInteractor: MapLicenseInteractor,
    private val elevationFixInteractor: ElevationFixInteractor
) : ViewModel() {
    private val dataStateFlow = MutableSharedFlow<DataState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow
    val orientationFlow: Flow<Double> = orientationSource.orientationFlow
    val elevationFixFlow: StateFlow<Int> = mapRepository.currentMapFlow.flatMapMerge {
        it?.getElevationFix() ?: MutableStateFlow(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val locationOrientationLayer: LocationOrientationLayer = LocationOrientationLayer(
        viewModelScope,
        settings,
        dataStateFlow,
        mapInteractor,
        onOutOfBounds = {
            snackBarController.showSnackBar(SnackBarEvent.CURRENT_LOCATION_OUT_OF_BOUNDS)
        }
    )

    val landmarkLayer: LandmarkLayer = LandmarkLayer(
        viewModelScope,
        dataStateFlow,
        mapInteractor
    )

    val markerLayer: MarkerLayer = MarkerLayer(
        viewModelScope,
        dataStateFlow,
        mapFeatureEvents.markerMoved,
        mapInteractor,
        onMarkerEdit = { marker, mapId, markerId ->
            mapFeatureEvents.postMarkerEditEvent(marker, mapId, markerId)
        }
    )

    val distanceLayer = DistanceLayer(
        viewModelScope,
        dataStateFlow.map { it.mapState }
    )

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
        mapInteractor,
        gpxRecordEvents
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

        // TODO: A map should have a StateFlow<List<Marker>>, just like it has a flow of Route.
        // That would eliminate the need to listen to app events, and allows for better separation
        // of concern.
        appEventBus.geoRecordImportEvent.map { event ->
            if (event is GeoRecordImportResult.GeoRecordImportOk) {
                if (event.newMarkersCount > 0) {
                    markerLayer.onMarkersChanged(event.map.id)
                }
            }
        }.launchIn(viewModelScope)
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
            landmarkLayer.onMarkerTap(mapState, map.id, id, x, y)
            markerLayer.onMarkerTap(mapState, map.id, id, x, y)
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
        fun onMarkerTap(mapState: MapState, mapId: Int, id: String, x: Double, y: Double)
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
