package com.peterlaurence.trekme.viewmodel.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

@HiltViewModel
class MapViewModel @Inject constructor(
    mapRepository: MapRepository,
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

    fun onLocationReceived(location: Location) {
        println("xxxx location $location")


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
}

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val isShowingOrientation: Boolean
) : UiState

object Loading : UiState
enum class Error : UiState {
    LicenseError, EmptyMap
}