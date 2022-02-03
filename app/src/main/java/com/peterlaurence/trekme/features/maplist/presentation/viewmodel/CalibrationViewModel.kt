package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.makeMapComposeTileStreamProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.ui.layout.Fill
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    mapRepository: MapRepository
): ViewModel() {
    private var mapState: MapState? = null

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        mapRepository.settingsMapFlow.map {
            if (it != null) {
                onMapChange(it)
            }
        }.launchIn(viewModelScope)
    }

    private fun onMapChange(map: Map) {
        /* Shutdown the previous map state, if any */
        mapState?.shutdown()

        /* For instance, MapCompose only supports levels of uniform tile size (and squared) */
        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            _uiState.value = EmptyMap
            return
        }

        val tileStreamProvider = makeMapComposeTileStreamProvider(map)

        val mapState = MapState(
            map.levelList.size,
            map.widthPx,
            map.heightPx,
            tileSize
        ) {
            highFidelityColors(false)
            minimumScaleMode(Fill)
        }.apply {
            addLayer(tileStreamProvider)
        }

        this.mapState = mapState
        _uiState.value = MapUiState(mapState)
    }
}

sealed interface UiState
object Loading : UiState
object EmptyMap : UiState
data class MapUiState(val mapState: MapState) : UiState