package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamReporter
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.mapSize
import com.peterlaurence.trekme.features.common.domain.util.toMapComposeTileStreamProvider
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.Config
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ScaleLimitsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignSpainConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ordnanceSurveyConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.osmConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.swissTopoConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.usgsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionMarker
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PartialExcursionRepository
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PendingSearchRepository
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.MarkerLayer
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer.RouteLayer
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.checkInternet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.disableFlingZoom
import ovh.plrapps.mapcompose.api.hasMarker
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.removeAllLayers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource,
    private val getTileStreamProviderDao: TileStreamProviderDao,
    private val pendingSearchRepository: PendingSearchRepository,
    private val partialExcursionRepository: PartialExcursionRepository,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor,
    getMapInteractor: GetMapInteractor,
    private val excursionRepository: ExcursionRepository
) : ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    private val _uiStateFlow = MutableStateFlow<UiState>(Loading)
    val uiStateFlow: StateFlow<UiState> = _uiStateFlow.asStateFlow()
    private val mapStateFlow = _uiStateFlow.filterIsInstance<MapReady>().map { it.mapState }

    private val mapSourceDataFlow = MutableStateFlow<MapSourceData>(OsmSourceData(Outdoors))

    private val _excursionItemsFlow = pendingSearchRepository.getSearchResultFlow().stateIn(
        viewModelScope, started = SharingStarted.Eagerly, initialValue = ResultL.loading()
    )

    val partialExcursionFlow = partialExcursionRepository.getPartialExcursionFlow().stateIn(
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
                partialExcursionRepository.postItem(item)
                _events.send(Event.OnMarkerClick)
            }
        }
    )

    val routeLayer = RouteLayer(
        scope = viewModelScope,
        geoRecordFlow = partialExcursionFlow.mapNotNull { it.getOrNull()?.geoRecord },
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

        viewModelScope.launch {
            if (!checkInternet()) {
                _events.send(Event.NoInternet)
            }
        }
    }

    fun onLocationReceived(location: Location) = viewModelScope.launch {
        /* If there is no MapState, no need to go further */
        val mapState = mapStateFlow.firstOrNull() ?: return@launch

        /* Project lat/lon off UI thread */
        val normalized = withContext(Dispatchers.Default) {
            wgs84ToNormalizedInteractor.getNormalized(location.latitude, location.longitude)
        }

        /* Update the position */
        if (normalized != null) {
            updatePosition(mapState, normalized.x, normalized.y)
        }
    }

    /**
     * Update the position on the map. The first time we update the position, we add the
     * position marker.
     * [x] and [y] are expected to be normalized coordinates.
     */
    private fun updatePosition(mapState: MapState, x: Double, y: Double) {
        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(positionMarkerId, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                PositionMarker()
            }
        }
    }

    /**
     * The user has selected a pin, and clicked on the download button in the bottomsheet.
     */
    fun onDownload(withMap: Boolean) = viewModelScope.launch {
        val partialExcursion = partialExcursionFlow.firstOrNull()?.getOrNull() ?: return@launch

        _events.send(Event.ExcursionDownloadStart)

        val result = excursionRepository.putExcursion(
            id = partialExcursion.searchItem.id,
            title = partialExcursion.searchItem.title,
            type = partialExcursion.searchItem.type,
            description = partialExcursion.searchItem.description,
            geoRecord = partialExcursion.geoRecord
        )

        when (result) {
            ExcursionRepository.PutExcursionResult.Ok,
            ExcursionRepository.PutExcursionResult.AlreadyExists,
            ExcursionRepository.PutExcursionResult.Pending -> { /* Do nothing */ }
            ExcursionRepository.PutExcursionResult.Error -> {
                _events.send(Event.ExcursionDownloadError)
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
            getTileStreamProviderDao.newTileStreamProvider(mapSourceData, makeReporter())
                .getOrNull()
        _uiStateFlow.value = if (tileStreamProvider != null) {
            mapState.removeAllLayers()
            mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
            MapReady(mapState)
        } else {
            Error.PROVIDER_OUTAGE
        }
    }

    private fun makeReporter(): TileStreamReporter {
        return object : TileStreamReporter {
            private var successCnt = AtomicInteger(0)
            private var failureCnt = AtomicInteger(0)

            override fun report(result: TileResult) {
                if (result is TileStream) {
                    if (result.tileStream != null) {
                        successCnt.incrementAndGet()
                    } else {
                        val failCnt = failureCnt.incrementAndGet()
                        val successCnt = successCnt.get()
                        check(failCnt, successCnt)
                    }
                }
            }

            private fun check(failCnt: Int, successCnt: Int) {
                if (failCnt > successCnt) {
                    _uiStateFlow.update {
                        Error.PROVIDER_OUTAGE
                    }
                }
            }
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
        object NoInternet : Event
        object ExcursionDownloadStart : Event
        object ExcursionDownloadError : Event
    }
}

private const val positionMarkerId = "position"

sealed interface UiState
object AwaitingLocation : UiState
object Loading : UiState
data class MapReady(val mapState: MapState) : UiState
enum class Error : UiState {
    PROVIDER_OUTAGE
}