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
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.TrackCreateScreenArgs
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.PointState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.TrackCreateLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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
    private val actionIdOnLastSave = MutableStateFlow<String?>(null)
    private var saveJob: Job? = null
    val savingState = MutableStateFlow(false)

    private val _events = Channel<Event>(1)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val map = mapRepository.getMap(UUID.fromString(args.mapId)) ?: return@launch
            configure(
                map,
                centroidX = args.centroidX,
                centroidY = args.centroidY,
                scale = args.scale,
                excursionId = args.excursionId
            )
        }
    }

    /* region map configuration */
    private suspend fun configure(map: Map, centroidX: Double, centroidY: Double, scale: Float, excursionId: String?) {
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

        val trackCreateLayer = TrackCreateLayer(viewModelScope, mapState)
        if (excursionId != null) {
            excursionRef = map.excursionRefs.value.firstOrNull { it.id == excursionId }

            var previous: NormalizedPos? = null
            var firstPointState: PointState? = null
            trackCreateInteractor.getCurrentRelativeCoordinates(map, excursionId).collect {
                val prev = previous
                if (prev != null) {
                    if (firstPointState == null) {
                        firstPointState = trackCreateLayer.addFirstSegment(prev.x, prev.y, it.x, it.y)
                    } else {
                        trackCreateLayer.addExistingPoint(it.x, it.y)
                    }
                }
                previous = it
            }
            firstPointState?.also {
                trackCreateLayer.initialize(it)
            }
        } else {
            trackCreateLayer.initializeNewTrack()
        }

        val shouldDisplaySave = combine(
            trackCreateLayer.lastUndoActionId,
            actionIdOnLastSave
        ) { lastUndoActionId, actionIdOnLastSave ->
            if (actionIdOnLastSave != null) {
                lastUndoActionId != actionIdOnLastSave
            } else {
                lastUndoActionId != null
            }
        }.stateIn(viewModelScope)

        val mapUiState = MapUiState(
            mapState = mapState,
            map = map,
            trackCreateLayer = trackCreateLayer,
            shouldDisplaySave = shouldDisplaySave
        )
        _uiState.value = mapUiState
    }
    /* endregion */

    fun getCurrentExcursionRef(): ExcursionRef? = excursionRef

    fun save(config: SaveConfig) {
        if (saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            val mapUiState = (_uiState.value as? MapUiState) ?: return@launch
            savingState.value = true
            actionIdOnLastSave.value = mapUiState.trackCreateLayer.lastUndoActionId.value

            /* Make a defensive copy of the current list of segments */
            val trackSegments = mapUiState.trackCreateLayer.trackState.value.toList()

            val geoRecordName = when (config) {
                is SaveConfig.UpdateExisting -> config.excursionRef.name.value
                is SaveConfig.CreateWithName -> config.name
            }

            val geoRecord = withContext(Dispatchers.Default) {
                val points = mutableSetOf<PointState>()
                for (s in trackSegments) {
                    points.add(s.p1)
                    points.add(s.p2)
                }
                val markers = points.map { p ->
                    val lonLat = getLonLatFromNormalizedCoordinateBlocking(
                        p.x,
                        p.y,
                        projection = mapUiState.map.projection,
                        mapBounds = mapUiState.map.mapBounds
                    )
                    Marker(id = p.id, lat = lonLat[1], lon = lonLat[0])
                }

                GeoRecord(
                    id = UUID.randomUUID(),
                    name = geoRecordName,
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

            when (config) {
                is SaveConfig.UpdateExisting -> {
                    trackCreateInteractor.saveGeoRecord(mapUiState.map, config.excursionRef, geoRecord)
                }
                is SaveConfig.CreateWithName -> {
                    excursionRef = trackCreateInteractor.createExcursion(
                        title = config.name,
                        geoRecord = geoRecord,
                        map = mapUiState.map
                    )
                }
            }

            savingState.value = false
            _events.send(Event.SaveDone)
        }
    }
}

sealed interface UiState
data class MapUiState(
    val mapState: MapState,
    val map: Map,
    val trackCreateLayer: TrackCreateLayer,
    val shouldDisplaySave: StateFlow<Boolean>
) : UiState

data object Loading : UiState

sealed interface SaveConfig {
    data class CreateWithName(val name: String): SaveConfig
    data class UpdateExisting(val excursionRef: ExcursionRef) : SaveConfig
}

sealed interface Event {
    data object SaveDone : Event
}