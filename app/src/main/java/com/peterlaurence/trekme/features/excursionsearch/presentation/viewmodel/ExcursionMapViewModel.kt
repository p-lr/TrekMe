package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.mapSize
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.Config
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ScaleLimitsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignSpainConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ordnanceSurveyConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.osmConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.swissTopoConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.usgsConfig
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.ExcursionGeoRecordRepository
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PendingSearchRepository
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.MarkerLayer
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.RouteLayer
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import com.peterlaurence.trekme.util.ResultL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.disableFlingZoom
import ovh.plrapps.mapcompose.api.removeAllLayers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource,
    private val getTileStreamProviderDao: TileStreamProviderDao,
    private val pendingSearchRepository: PendingSearchRepository,
    private val excursionGeoRecordRepository: ExcursionGeoRecordRepository,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor,
    getMapInteractor: GetMapInteractor
) : ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    private val _uiStateFlow = MutableStateFlow<UiState>(Loading)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow.asStateFlow()
    private val mapStateFlow = _uiStateFlow.filterIsInstance<MapReady>().map { it.mapState }

    private val mapSourceDataFlow = MutableStateFlow<MapSourceData>(OsmSourceData(Outdoors))

    private val _excursionItemsFlow = pendingSearchRepository.getSearchResultFlow().stateIn(
        viewModelScope, started = SharingStarted.Eagerly, initialValue = ResultL.loading()
    )

    val geoRecordFlow = excursionGeoRecordRepository.getGeoRecordFlow().stateIn(
        viewModelScope, started = SharingStarted.Eagerly, initialValue = ResultL.loading()
    )

    private val _events = Channel<Event>(1)
    val event = _events.receiveAsFlow()

    val markerLayer = MarkerLayer(
        scope = viewModelScope,
        excursionItemsFlow = _excursionItemsFlow.mapNotNull { it.getOrNull() },
        mapStateFlow = mapStateFlow,
        wgs84ToNormalizedInteractor = wgs84ToNormalizedInteractor,
        onExcursionItemClick = { item ->
            viewModelScope.launch {
                excursionGeoRecordRepository.postItem(item)
                _events.send(Event.OnMarkerClick)
            }
        }
    )

    val routeLayer = RouteLayer(
        scope = viewModelScope,
        geoRecordFlow = geoRecordFlow.mapNotNull { it.getOrNull() },
        mapStateFlow = mapStateFlow,
        wgs84ToNormalizedInteractor = wgs84ToNormalizedInteractor,
        getMapInteractor = getMapInteractor
    )

    init {
        viewModelScope.launch {
            mapSourceDataFlow.collect {
                updateMapState(it)
            }
        }
    }

    private suspend fun updateMapState(mapSourceData: MapSourceData) = coroutineScope {
        val previousMapState = _uiStateFlow.value.getMapState()

        /* Shutdown the previous MapState, if any */
        previousMapState?.shutdown()

        /* Display the loading screen while building the new MapState */
        _uiStateFlow.value = Loading

        val wmtsConfig = getWmtsConfig(mapSourceData)
        val initScaleAndScroll = if (previousMapState != null) {
            Pair(
                previousMapState.scale,
                NormalizedPos(previousMapState.centroidY, previousMapState.centroidY)
            )
        } else {
            _uiStateFlow.value = AwaitingLocation
            val latLon = pendingSearchRepository.locationFlow.filterNotNull().first()
            _uiStateFlow.value = Loading
            val normalized = wgs84ToNormalizedInteractor.getNormalized(latLon.lat, latLon.lon)
            if (normalized != null) {
                Pair(0.0625f, normalized)
            } else {
                // TODO: error could not get location
                return@coroutineScope
            }
        }

        val mapState = MapState(
            19, wmtsConfig.mapSize, wmtsConfig.mapSize,
            tileSize = wmtsConfig.tileSize,
            workerCount = 16
        ) {
            /* Apply configuration */
            val mapConfiguration = getScaleAndScrollConfig(mapSourceData)
            mapConfiguration.forEach { conf ->
                when (conf) {
                    is ScaleLimitsConfig -> {
                        val minScale = conf.minScale
                        if (minScale == null) {
                            minimumScaleMode(Fit)
                        } else {
                            minimumScaleMode(Forced(minScale))
                        }
                        conf.maxScale?.also { maxScale -> maxScale(maxScale) }
                    }

                    else -> {} /* Nothing to do */
                }
            }

            scale(initScaleAndScroll.first)
            scroll(x = initScaleAndScroll.second.x, y = initScaleAndScroll.second.y)

            magnifyingFactor(1)
        }.apply {
            disableFlingZoom()
            /* Use grey background to contrast with the material 3 top app bar in light mode */
            setMapBackground(Color(0xFFF8F8F8))
        }

        val tileStreamProvider =
            getTileStreamProviderDao.newTileStreamProvider(mapSourceData).getOrNull()
        _uiStateFlow.value = if (tileStreamProvider != null) {
            mapState.removeAllLayers()
//            mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
            MapReady(mapState)
        } else {
            Error.PROVIDER_OUTAGE
        }
    }

    private fun UiState.getMapState(): MapState? {
        return when (this) {
            is MapReady -> mapState
            else -> null
        }
    }

    private fun getScaleAndScrollConfig(mapSourceData: MapSourceData): List<Config> {
        return when (mapSourceData) {
            is IgnSourceData -> ignConfig
            IgnSpainData -> ignSpainConfig
            OrdnanceSurveyData -> ordnanceSurveyConfig
            is OsmSourceData -> osmConfig
            SwissTopoData -> swissTopoConfig
            UsgsData -> usgsConfig
        }
    }

    private fun getWmtsConfig(mapSourceData: MapSourceData): WmtsConfig {
        return when (mapSourceData) {
            is OsmSourceData -> {
                when (mapSourceData.layer) {
                    Outdoors -> WmtsConfig(512, mapSize * 2)
                    else -> WmtsConfig(256, mapSize)
                }
            }

            else -> WmtsConfig(256, mapSize)
        }
    }

    private data class WmtsConfig(val tileSize: Int, val mapSize: Int)

    sealed interface Event {
        object OnMarkerClick : Event
    }
}

sealed interface UiState
object AwaitingLocation : UiState
object Loading : UiState
data class MapReady(val mapState: MapState) : UiState
enum class Error : UiState {
    PROVIDER_OUTAGE
}