package com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.TrackCreateScreenArgs
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TrackCreateViewModel @Inject constructor(
    private val mapComposeTileStreamProviderInteractor: MapComposeTileStreamProviderInteractor,
    val settings: Settings,
    val mapRepository: MapRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.toRoute<TrackCreateScreenArgs>()
    private val dataStateFlow = MutableSharedFlow<DataState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val map = mapRepository.getMap(UUID.fromString(args.mapId)) ?: return@launch
            onMapChange(
                map,
                centroidX = args.centroidX,
                centroidY = args.centroidY,
                scale = args.scale
            )
        }
    }

    /* region map configuration */
    private suspend fun onMapChange(map: Map, centroidX: Double, centroidY: Double, scale: Float) {
        /* Shutdown the previous map state, if any */
        dataStateFlow.replayCache.firstOrNull()?.mapState?.shutdown()

        /* For instance, MapCompose only supports levels of uniform tile size (and squared) */
        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            /* This case should not be possible because we're coming from a working map */
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
            scale(scale)
            scroll(centroidX, centroidY)
        }.apply {
            addLayer(tileStreamProvider)
            setScrollOffsetRatio(0.5f, 0.5f)
        }

        dataStateFlow.tryEmit(DataState(map, mapState))
        val mapUiState = MapUiState(
            mapState = mapState,
            mapNameFlow = map.name
        )
        _uiState.value = mapUiState
    }
    /* endregion */
}

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val mapNameFlow: StateFlow<String>
) : UiState

data object Loading : UiState