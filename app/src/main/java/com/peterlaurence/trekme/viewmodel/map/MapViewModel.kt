package com.peterlaurence.trekme.viewmodel.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.model.OrientationSource
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapLoader: MapLoader,
    private val mapRepository: MapRepository,
    locationSource: LocationSource,
    orientationSource: OrientationSource,
    private val settings: Settings,
    private val appEventBus: AppEventBus
) : ViewModel() {
    private var mapState: MapState? = null

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _topBarState = MutableStateFlow(TopBarState())
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow
    val orientationFlow: Flow<Double> = orientationSource.orientationFlow

    private val locationLayer: LocationLayer = LocationLayer(viewModelScope, settings, mapRepository, uiState)
    private val landmarkLayer: LandmarkLayer = LandmarkLayer(viewModelScope, mapLoader, mapRepository, uiState)

    val snackBarController = SnackBarController()

    init {
        mapRepository.mapFlow.map {
            if (it != null) {
                onMapChange(it)
            }
        }.launchIn(viewModelScope)

        settings.getRotationMode().map { rotMode ->
            mapState?.also { mapState ->
                applyRotationMode(mapState, rotMode)
            }
        }.launchIn(viewModelScope)

        settings.getOrientationVisibility().map { visibility ->
            val uiState = _uiState.value
            if (uiState is MapUiState) {
                _uiState.value = uiState.copy(isShowingOrientation = visibility)
            }
        }.launchIn(viewModelScope)
    }

    /* region TopAppBar events */
    fun onMainMenuClick() {
        appEventBus.openDrawer()
    }
    /* endregion */

    /* region Location layer */
    fun onLocationReceived(location: Location) {
        locationLayer.updateMapUi(location)
    }

    fun toggleShowOrientation() = viewModelScope.launch {
        settings.toggleOrientationVisibility()
    }

    fun setOrientation(intrinsicAngle: Double, displayRotation: Int) {
        locationLayer.onOrientation(intrinsicAngle, displayRotation)
    }
    /* endregion */

    /* region map configuration */
    private suspend fun onMapChange(map: Map) {
        /* Shutdown the previous map state, if any */
        mapState?.shutdown()

        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            _uiState.value = Error.EmptyMap
            return
        }

        val tileStreamProvider = makeTileStreamProvider(map)

        val magnifyingFactor = settings.getMagnifyingFactor().first()

        val mapState = MapState(
            map.levelList.size,
            map.widthPx,
            map.heightPx,
            tileStreamProvider,
            tileSize,
            magnifyingFactor = magnifyingFactor
        )

        /* region Configuration */
        val maxScale = settings.getMaxScale().first()
        mapState.maxScale = maxScale

        val rotationMode = settings.getRotationMode().first()
        applyRotationMode(mapState, rotationMode)

        mapState.shouldLoopScale = true
        /* endregion */

        this.mapState = mapState
        _uiState.value = MapUiState(
            mapState,
            isShowingOrientation = settings.getOrientationVisibility().first()
        )
    }

    private fun applyRotationMode(mapState: MapState, rotationMode: RotationMode) {
        when (rotationMode) {
            RotationMode.NONE -> {
                mapState.rotation = 0f
                mapState.disableRotation()
            }
            RotationMode.FOLLOW_ORIENTATION -> {
                mapState.disableRotation()
            }
            RotationMode.FREE -> mapState.enableRotation()
        }
    }

    private fun makeTileStreamProvider(map: Map): MapComposeTileStreamProvider {
        return MapComposeTileStreamProvider { row, col, zoomLvl ->
            val relativePathString =
                "$zoomLvl${File.separator}$row${File.separator}$col${map.imageExtension}"

            try {
                FileInputStream(File(map.directory, relativePathString))
            } catch (e: Exception) {
                null
            }
        }
    }
    /* endregion */
}

data class TopBarState(val isShowingOrientation: Boolean = false)

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val isShowingOrientation: Boolean
) : UiState

object Loading : UiState
enum class Error : UiState {
    LicenseError, EmptyMap
}

enum class SnackBarEvent {
    CURRENT_LOCATION_OUT_OF_BOUNDS
}
