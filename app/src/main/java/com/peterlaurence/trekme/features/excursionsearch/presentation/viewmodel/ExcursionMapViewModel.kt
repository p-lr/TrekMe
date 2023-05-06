package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OpenTopoMap
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
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
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PendingSearchRepository
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.Wgs84ToNormalizedInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.disableFlingZoom
import ovh.plrapps.mapcompose.api.removeAllLayers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setMapBackground
import ovh.plrapps.mapcompose.api.setScroll
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource,
    private val getTileStreamProviderDao: TileStreamProviderDao,
    private val pendingSearchRepository: PendingSearchRepository,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor
) : ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    private val _mapStateFlow = MutableStateFlow<UiState>(Loading)
    val mapStateFlow: StateFlow<UiState> = _mapStateFlow.asStateFlow()

    private val mapSourceDataFlow = MutableStateFlow<MapSourceData>(OsmSourceData(OpenTopoMap))

    private val tileStreamProviderFlow = mapSourceDataFlow.map {
        getTileStreamProviderDao.newTileStreamProvider(it)
    }

    init {
        viewModelScope.launch {
            mapSourceDataFlow.collect {
                updateMapState(it)
            }
        }

        viewModelScope.launch {
            val res = pendingSearchRepository.search()
            // TODO : use result to produce state
        }
    }

    private suspend fun updateMapState(mapSourceData: MapSourceData) = coroutineScope {
        val previousMapState = _mapStateFlow.value.getMapState()

        /* Shutdown the previous MapState, if any */
        previousMapState?.shutdown()

        /* Display the loading screen while building the new MapState */
        _mapStateFlow.value = Loading

        val mapState = MapState(
            19, mapSize, mapSize,
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
        }.apply {
            disableFlingZoom()
            /* Use grey background to contrast with the material 3 top app bar in light mode */
            setMapBackground(Color(0xFFF8F8F8))
        }

        if (previousMapState != null) {
            mapState.scale = previousMapState.scale
            launch {
                mapState.setScroll(previousMapState.scroll)
            }
        } else {
            _mapStateFlow.value = AwaitingLocation
            val latLon = pendingSearchRepository.locationFlow.filterNotNull().first()
            _mapStateFlow.value = Loading
            val normalized = wgs84ToNormalizedInteractor.getNormalized(latLon.lat, latLon.lon)
            if (normalized != null) {
                launch {
                    mapState.scrollTo(
                        x = normalized.x,
                        y = normalized.y,
                        destScale = 0.0625f  // Level 15
                    )
                }
            } else {
                // TODO: error could not get location
            }
        }

        tileStreamProviderFlow.collect { result ->
            val tileStreamProvider = result.getOrNull()
            _mapStateFlow.value = if (tileStreamProvider != null) {
                mapState.removeAllLayers()
                mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
                MapReady(mapState)
            } else {
                Error.PROVIDER_OUTAGE
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
}

sealed interface UiState
object AwaitingLocation : UiState
object Loading : UiState
data class MapReady(val mapState: MapState) : UiState
enum class Error : UiState {
    PROVIDER_OUTAGE
}