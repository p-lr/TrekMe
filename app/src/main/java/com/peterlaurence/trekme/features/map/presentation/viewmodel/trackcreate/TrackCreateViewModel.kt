package com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.RouteGroup
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.map.domain.core.getLonLatFromNormalizedCoordinateBlocking
import com.peterlaurence.trekme.features.map.domain.interactors.TrackCreateInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.TrackCreateScreenArgs
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.TrackCreateLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val trackCreateInteractor: TrackCreateInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.toRoute<TrackCreateScreenArgs>()
    private val dataStateFlow = MutableSharedFlow<DataState>(1, 0, BufferOverflow.DROP_OLDEST)

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var excursionRef: ExcursionRef? = null

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

        dataStateFlow.emit(DataState(map, mapState))
        val mapUiState = MapUiState(
            mapState = mapState,
            map = map,
            trackCreateLayer = TrackCreateLayer(viewModelScope, mapState)
        )
        _uiState.value = mapUiState
    }
    /* endregion */

    fun isNewTrack(): Boolean = excursionRef == null

    fun save(withName: String) = viewModelScope.launch {
        // TODO: add a loading while saving
        val mapUiState = (_uiState.value as? MapUiState) ?: return@launch

        /* Make a defensive copy of the current list of segments */
        val trackSegments = mapUiState.trackCreateLayer.trackState.value.toList()

        val geoRecord = withContext(Dispatchers.Default) {
            val markers = buildList {
                for (s in trackSegments) {
                    val lonLatP1 = getLonLatFromNormalizedCoordinateBlocking(
                        s.p1.x,
                        s.p1.y,
                        projection = mapUiState.map.projection,
                        mapBounds = mapUiState.map.mapBounds
                    )

                    add(Marker(id = s.p1.id, lat = lonLatP1[1], lon = lonLatP1[0]))

                    val lonLatP2 = getLonLatFromNormalizedCoordinateBlocking(
                        s.p2.x,
                        s.p2.y,
                        projection = mapUiState.map.projection,
                        mapBounds = mapUiState.map.mapBounds
                    )

                    add(Marker(id = s.p2.id, lat = lonLatP2[1], lon = lonLatP2[0]))
                }
            }

            GeoRecord(
                id = UUID.randomUUID(),
                name = withName,
                routeGroups = listOf(
                    RouteGroup(
                        id = UUID.randomUUID().toString(),
                        routes = listOf(
                            Route(
                                id = UUID.randomUUID().toString(),
                                initialMarkers = markers,
                            )
                        )
                    )
                ),
                markers = emptyList(),
                time = null,
                elevationSourceInfo = null
            )
        }

        excursionRef = trackCreateInteractor.createExcursion(
            title = withName,
            geoRecord = geoRecord,
            map = mapUiState.map
        )
    }
}

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val map: Map,
    val trackCreateLayer: TrackCreateLayer
) : UiState

data object Loading : UiState