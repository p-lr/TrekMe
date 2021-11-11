package com.peterlaurence.trekme.viewmodel.map

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapBounds
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.ui.common.PositionMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    locationSource: LocationSource,
    private val settings: Settings,
) : ViewModel() {
    private var mapState: MapState? = null

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val locationFlow: Flow<Location> = locationSource.locationFlow

    init {
        mapRepository.mapFlow.map {
            if (it != null) onMapChange(it)
        }.launchIn(viewModelScope)

        settings.getRotationMode().map { rotMode ->
            mapState?.also { mapState ->
                applyRotationMode(mapState, rotMode)
            }
        }.launchIn(viewModelScope)
    }

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

    suspend fun onLocationReceived(location: Location) {
        /* If there is no MapState, no need to go further */
        val mapState = this.mapState ?: return
        val map = mapRepository.mapFlow.firstOrNull() ?: return

        viewModelScope.launch {
            /* Project lat/lon off UI thread */
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(location.latitude, location.longitude)
            }

            /* Update the position */
            val mapBounds = map.mapBounds
            if (projectedValues != null && mapBounds != null) {
                updatePosition(mapState, mapBounds, projectedValues[0], projectedValues[1])
            }
        }
    }

    /**
     * Update the position on the map. The first time we update the position, we add the
     * position marker.
     *
     * @param X the projected X coordinate
     * @param Y the projected Y coordinate
     */
    private fun updatePosition(mapState: MapState, mapBounds: MapBounds, X: Double, Y: Double) {
        val x = normalize(X, mapBounds.X0, mapBounds.X1)
        val y = normalize(Y, mapBounds.Y0, mapBounds.Y1)

        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(positionMarkerId, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                PositionMarker()
            }
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

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private const val positionMarkerId = "position"

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val isShowingOrientation: Boolean
) : UiState

object Loading : UiState
enum class Error : UiState {
    LicenseError, EmptyMap
}