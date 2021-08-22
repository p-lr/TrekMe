package com.peterlaurence.trekme.viewmodel.mapcreate

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.BoundingBox
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.map.contains
import com.peterlaurence.trekme.core.mapsource.*
import com.peterlaurence.trekme.core.mapsource.wmts.getMapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.core.providers.stream.TileStreamProviderOverlay
import com.peterlaurence.trekme.core.providers.stream.newTileStreamProvider
import com.peterlaurence.trekme.repositories.api.IgnApiRepository
import com.peterlaurence.trekme.repositories.api.OrdnanceSurveyApiRepository
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerOverlayRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties
import com.peterlaurence.trekme.repositories.mapcreate.WmtsSourceRepository
import com.peterlaurence.trekme.service.DownloadService
import com.peterlaurence.trekme.service.event.DownloadMapRequest
import com.peterlaurence.trekme.ui.common.PositionMarker
import com.peterlaurence.trekme.ui.mapcreate.dialogs.DownloadFormDataBundle
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.GoogleMapWmtsViewFragment
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components.AreaUiController
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components.PositionMarker
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.model.Point
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.model.toDomain
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.toMapComposeTileStreamProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.net.URL
import javax.inject.Inject

/**
 * View-model for [GoogleMapWmtsViewFragment]. It takes care of:
 * * storing the predefined init scale and position for each [WmtsSource]
 * * keeping track of the layer (as to each [WmtsSource] may correspond multiple layers)
 * * providing a [TileStreamProvider] for the fragment
 *
 * @author P.Laurence on 09/11/19
 */
@HiltViewModel
class GoogleMapWmtsViewModel @Inject constructor(
    private val app: Application,
    private val downloadRepository: DownloadRepository,
    private val ignApiRepository: IgnApiRepository,
    private val wmtsSourceRepository: WmtsSourceRepository,
    private val ordnanceSurveyApiRepository: OrdnanceSurveyApiRepository,
    private val layerOverlayRepository: LayerOverlayRepository,
    private val mapCreateEventBus: MapCreateEventBus,
    private val locationSource: LocationSource
) : ViewModel() {
    private val _states = MutableStateFlow<WmtsState>(Loading)
    val state: StateFlow<WmtsState> = _states.asStateFlow()

    val eventListState = mutableStateListOf<WmtsEvent>()

    private val defaultIgnLayer: IgnLayer = IgnClassic
    private val defaultOsmLayer: OsmLayer = WorldStreetMap

    private val _wmtsSourceAccessibility = MutableStateFlow(true)
    val wmtsSourceAccessibility = _wmtsSourceAccessibility.asStateFlow()

    private val areaController = AreaUiController()

    private val scaleAndScrollInitConfig = mapOf(
        WmtsSource.IGN to listOf(
            ScaleLimitsConfig(maxScale = 0.5f),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 17),
            BoundariesConfig(
                listOf(
                    BoundingBox(41.21, 51.05, -4.92, 8.37),        // France
                    BoundingBox(-21.39, -20.86, 55.20, 55.84),     // La Réunion
                    BoundingBox(2.07, 5.82, -54.66, -51.53),       // Guyane
                    BoundingBox(15.82, 16.54, -61.88, -60.95),     // Guadeloupe
                    BoundingBox(18.0, 18.135, -63.162, -62.965),   // St Martin
                    BoundingBox(17.856, 17.988, -62.957, -62.778), // St Barthélemy
                    BoundingBox(14.35, 14.93, -61.31, -60.75),     // Martinique
                    BoundingBox(-17.945, -17.46, -149.97, -149.1), // Tahiti
                )
            )
        ),
        WmtsSource.OPEN_STREET_MAP to listOf(
            ScaleLimitsConfig(maxScale = 0.25f),
            BoundariesConfig(
                listOf(
                    BoundingBox(-80.0, 83.0, -180.0, 180.0)        // World
                )
            )
        ),
        WmtsSource.USGS to listOf(
            ScaleLimitsConfig(maxScale = 0.25f),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 16),
            BoundariesConfig(
                listOf(
                    BoundingBox(24.69, 49.44, -124.68, -66.5)
                )
            )
        ),
        WmtsSource.SWISS_TOPO to listOf(
            InitScaleAndScrollConfig(0.0006149545f, 21064, 13788),
            ScaleLimitsConfig(minScale = 0.0006149545f, maxScale = 0.5f),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 17),
            BoundariesConfig(
                listOf(
                    BoundingBox(45.78, 47.838, 5.98, 10.61)
                )
            )
        ),
        WmtsSource.IGN_SPAIN to listOf(
            InitScaleAndScrollConfig(0.0003546317f, 11127, 8123),
            ScaleLimitsConfig(minScale = 0.0003546317f, maxScale = 0.5f),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 17),
            BoundariesConfig(
                listOf(
                    BoundingBox(35.78, 43.81, -9.55, 3.32)
                )
            )
        ),
        WmtsSource.ORDNANCE_SURVEY to listOf(
            InitScaleAndScrollConfig(0.000830759f, 27011, 17261),
            ScaleLimitsConfig(minScale = 0.000830759f, maxScale = 0.25f),
            LevelLimitsConfig(7, 16),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            BoundariesConfig(
                listOf(
                    BoundingBox(49.8, 61.08, -8.32, 2.04)
                )
            )
        )
    )

    private val activePrimaryLayerForSource: MutableMap<WmtsSource, Layer> = mutableMapOf(
        WmtsSource.IGN to defaultIgnLayer
    )

    private val projection = MercatorProjection()

    init {
        wmtsSourceRepository.wmtsSourceState.map { source ->
            source?.also { updateMapState(source) }
        }.launchIn(viewModelScope)

        mapCreateEventBus.layerSelectEvent.map {
            onPrimaryLayerDefined(it)
        }.launchIn(viewModelScope)
    }

    fun toggleArea() {
        viewModelScope.launch {
            val nextState = when (val st = state.value) {
                is AreaSelection -> {
                    areaController.removeArea(st.mapState)
                    MapReady(st.mapState)
                }
                Loading, is WmtsError -> null
                is MapReady -> {
                    areaController.addArea(st.mapState)
                    AreaSelection(st.mapState, areaController)
                }
            } ?: return@launch

            _states.value = nextState
        }
    }

    fun acknowledgeError() {
        eventListState.removeFirstOrNull()
    }

    private fun updateMapState(wmtsSource: WmtsSource, formerProperties: FormerProperties? = null) {
        viewModelScope.launch {
            /* Shutdown the previous MapState, if any */
            state.value.getMapState()?.shutdown()

            /* Display the loading screen while building the new MapState */
            _states.value = Loading

            val tileStreamProvider = runCatching {
                createTileStreamProvider(wmtsSource)
            }.onFailure {
                // TODO: Couldn't fetch API key. The VPS might be down.
                // Do the equivalent of former showVpsFailureMessage() on the fragment
            }.getOrNull() ?: return@launch

            val mapState = MapState(
                19, mapSize, mapSize,
                tileStreamProvider = tileStreamProvider.toMapComposeTileStreamProvider(),
                workerCount = 16
            )

            /* Apply configuration */
            val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
            mapConfiguration?.forEach { conf ->
                when (conf) {
                    is ScaleLimitsConfig -> {
                        val minScale = conf.minScale
                        if (minScale == null) {
                            mapState.minimumScaleMode = Fit
                            mapState.scale = 0f
                        } else {
                            mapState.minimumScaleMode = Forced(minScale)
                        }
                        conf.maxScale?.also { maxScale -> mapState.maxScale = maxScale }
                    }
                    is InitScaleAndScrollConfig -> {
                        mapState.scale = conf.scale
                        launch {
                            mapState.setScroll(
                                Offset(
                                    conf.scrollX.toFloat(),
                                    conf.scrollY.toFloat()
                                )
                            )
                        }
                    }
                    else -> { /* Nothing to do */
                    }
                }
            }

            /* Apply former settings, if any */
            if (formerProperties != null) {
                mapState.scale = formerProperties.scale
                launch {
                    mapState.setScroll(formerProperties.scroll)
                }
            }

            // TODO: add magnifying factor api to MapCompose
            if (wmtsSource == WmtsSource.OPEN_STREET_MAP) {
                // mapState.setMagnifyingFactor(1)
            }

            _states.value = MapReady(mapState)

            /* Restore the location marker right now - even if subsequent updates will do it anyway. */
            updatePositionOneTime()
        }
    }

    fun getScaleAndScrollConfig(wmtsSource: WmtsSource): List<Config>? {
        return scaleAndScrollInitConfig[wmtsSource]
    }

    fun getAvailablePrimaryLayersForSource(wmtsSource: WmtsSource): List<Layer>? {
        return when (wmtsSource) {
            WmtsSource.IGN -> ignLayersPrimary
            WmtsSource.OPEN_STREET_MAP -> osmLayersPrimary
            else -> null
        }
    }

    /**
     * Returns the active layer for the given source.
     */
    fun getActivePrimaryLayerForSource(wmtsSource: WmtsSource): Layer? {
        return when (wmtsSource) {
            WmtsSource.IGN -> getActivePrimaryIgnLayer()
            WmtsSource.OPEN_STREET_MAP -> getActivePrimaryOsmLayer()
            else -> null
        }
    }

    private fun getActivePrimaryIgnLayer(): Layer {
        return activePrimaryLayerForSource[WmtsSource.IGN] ?: defaultIgnLayer
    }

    private fun getActivePrimaryOsmLayer(): Layer {
        return activePrimaryLayerForSource[WmtsSource.OPEN_STREET_MAP] ?: defaultOsmLayer
    }

    private fun onPrimaryLayerDefined(layerId: String) {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return
        setPrimaryLayerForSourceFromId(wmtsSource, layerId)

        val formerProperties = state.value.getMapState()?.let {
            FormerProperties(it.scale, it.scroll)
        }
        updateMapState(wmtsSource, formerProperties)

        /* Restore the location marker right now - even if subsequent updates will do it anyway. */
        updatePositionOneTime()
    }

    private fun updatePositionOneTime() {
        locationSource.locationFlow.take(1).map {
            onLocationReceived(it)
        }.launchIn(viewModelScope)
    }

    fun setPrimaryLayerForSourceFromId(wmtsSource: WmtsSource, layerId: String) {
        val layer = getPrimaryLayer(layerId)
        if (layer != null) {
            activePrimaryLayerForSource[wmtsSource] = layer
        }
    }

    /**
     * Returns the active overlay layers for the given source.
     */
    private fun getOverlayLayersForSource(wmtsSource: WmtsSource): List<LayerProperties> {
        return layerOverlayRepository.getLayerProperties(wmtsSource)
    }

    /**
     * Creates the [TileStreamProvider] for the given source. If we couldn't fetch the API key (when
     * we should have been able to do so), an [IllegalStateException] is thrown.
     */
    @Throws(ApiFetchError::class)
    suspend fun createTileStreamProvider(wmtsSource: WmtsSource): TileStreamProvider {
        val mapSourceData = when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = getActivePrimaryIgnLayer()
                val overlays = getOverlayLayersForSource(wmtsSource)
                val ignApi = ignApiRepository.getApi() ?: throw ApiFetchError()
                IgnSourceData(ignApi, layer, overlays)
            }
            WmtsSource.ORDNANCE_SURVEY -> {
                val api = ordnanceSurveyApiRepository.getApi() ?: throw ApiFetchError()
                OrdnanceSurveyData(api)
            }
            WmtsSource.OPEN_STREET_MAP -> {
                val layer = getActivePrimaryOsmLayer()
                OsmSourceData(layer)
            }
            WmtsSource.SWISS_TOPO -> SwissTopoData
            WmtsSource.USGS -> UsgsData
            WmtsSource.IGN_SPAIN -> IgnSpainData
        }
        return newTileStreamProvider(mapSourceData).also {
            /* Don't test the stream provider if it has overlays */
            if (it !is TileStreamProviderOverlay) {
                checkTileAccessibility(wmtsSource, it)
            }
        }
    }

    private suspend fun getApi(urlStr: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(urlStr)
            val connection = url.openConnection()
            connection.getInputStream().bufferedReader().use {
                it.readText()
            }
        }.getOrNull()
    }

    fun onValidateArea() {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)

        /* Specifically for Cadastre IGN layer, set the startMaxLevel to 17 */
        val hasCadastreOverlay = getOverlayLayersForSource(wmtsSource).any { layerProp ->
            layerProp.layer is Cadastre
        }
        val startMaxLevel = if (hasCadastreOverlay) 17 else null

        /* Otherwise, honor the level limits configuration for this source, if any. */
        val levelConf =
            mapConfiguration?.firstOrNull { conf -> conf is LevelLimitsConfig } as? LevelLimitsConfig

        /* At this point, the current state should be AreaSelection */
        val areaSelectionState = _states.value as? AreaSelection ?: return

        fun interpolate(t: Double, min: Double, max: Double) = min + t * (max - min)
        val p1 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p1x, x0, x1),
                interpolate(p1y, y0, y1)
            )
        }
        val p2 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p2x, x0, x1),
                interpolate(p2y, y0, y1)
            )
        }

        val mapSourceBundle = if (levelConf != null) {
            if (startMaxLevel != null) {
                DownloadFormDataBundle(
                    wmtsSource,
                    p1,
                    p2,
                    levelConf.levelMin,
                    levelConf.levelMax,
                    startMaxLevel
                )
            } else {
                DownloadFormDataBundle(wmtsSource, p1, p2, levelConf.levelMin, levelConf.levelMax)
            }
        } else {
            DownloadFormDataBundle(wmtsSource, p1, p2)
        }

        mapCreateEventBus.showDownloadForm(mapSourceBundle)
    }

    /**
     * We start the download with the [DownloadService]. The download request is posted to the
     * relevant repository before the service is started.
     * The service then process the request when it starts.
     */
    fun onDownloadFormConfirmed(
        wmtsSource: WmtsSource,
        p1: Point, p2: Point, minLevel: Int, maxLevel: Int
    ) {
        val mapSpec = getMapSpec(minLevel, maxLevel, p1.toDomain(), p2.toDomain())
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1.toDomain(), p2.toDomain())
        viewModelScope.launch {
            val tileStreamProvider = runCatching {
                createTileStreamProvider(wmtsSource)
            }.getOrNull() ?: return@launch
            val request = DownloadMapRequest(wmtsSource, mapSpec, tileCount, tileStreamProvider)
            downloadRepository.postDownloadMapRequest(request)
            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
    }

    /**
     * Simple check whether we are able to download tiles or not.
     */
    private fun checkTileAccessibility(
        wmtsSource: WmtsSource,
        tileStreamProvider: TileStreamProvider
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _wmtsSourceAccessibility.value = when (wmtsSource) {
                WmtsSource.IGN -> {
                    try {
                        checkIgnProvider(tileStreamProvider)
                    } catch (e: Exception) {
                        false
                    }
                }
                WmtsSource.IGN_SPAIN -> checkIgnSpainProvider(tileStreamProvider)
                WmtsSource.USGS -> checkUSGSProvider(tileStreamProvider)
                WmtsSource.OPEN_STREET_MAP -> checkOSMProvider(tileStreamProvider)
                WmtsSource.SWISS_TOPO -> checkSwissTopoProvider(tileStreamProvider)
                WmtsSource.ORDNANCE_SURVEY -> checkOrdnanceSurveyProvider(tileStreamProvider)
            }
        }
    }

    fun onLocationReceived(location: Location) {
        /* If there is no MapState, no need to go further */
        val mapState = _states.value.getMapState() ?: return

        viewModelScope.launch {
            /* Project lat/lon off UI thread */
            val projectedValues = withContext(Dispatchers.Default) {
                projection.doProjection(location.latitude, location.longitude)
            }

            /* Update the position */
            if (projectedValues != null) {
                updatePosition(mapState, projectedValues[0], projectedValues[1])
            }
        }
    }

    /**
     * Update the position on the map. The first time we update the position, we add the
     * [PositionMarker].
     *
     * @param X the projected X coordinate
     * @param Y the projected Y coordinate
     */
    private fun updatePosition(mapState: MapState, X: Double, Y: Double) {
        val x = normalize(X, x0, x1)
        val y = normalize(Y, y0, y1)

        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(positionMarkerId, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                PositionMarker()
            }
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    fun zoomOnPosition() {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        locationSource.locationFlow.take(1).map { location ->
            val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
            val boundaryConf = mapConfiguration?.filterIsInstance<BoundariesConfig>()?.firstOrNull()
            boundaryConf?.boundingBoxList?.also { boxes ->
                if (boxes.contains(location.latitude, location.longitude)) {
                    centerOnPosition()
                } else {
                    eventListState.add(WmtsEvent.OUT_OF_BOUNDS)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun centerOnPosition() {
        val mapState = _states.value.getMapState() ?: return
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val scaleConf =
            mapConfiguration?.filterIsInstance<ScaleForZoomOnPositionConfig>()?.firstOrNull()

        viewModelScope.launch {
            /* If we have conf, use it. Otherwise, use 1f - the scale is caped by the max scale anyway */
            mapState.centerOnMarker(positionMarkerId, scaleConf?.scale ?: 1f)
        }
    }
}

private const val positionMarkerId = "position"

/* Size of level 18 (levels are 0-based) */
private const val mapSize = 67108864

private const val x0 = -20037508.3427892476320267
private const val y0 = -x0
private const val x1 = -x0
private const val y1 = x0

sealed class Config
data class InitScaleAndScrollConfig(val scale: Float, val scrollX: Int, val scrollY: Int) : Config()
data class ScaleForZoomOnPositionConfig(val scale: Float) : Config()
data class ScaleLimitsConfig(val minScale: Float? = null, val maxScale: Float? = null) : Config()
data class LevelLimitsConfig(val levelMin: Int = 1, val levelMax: Int = 18) : Config()
data class BoundariesConfig(val boundingBoxList: List<BoundingBox>) : Config()

fun List<BoundingBox>.contains(latitude: Double, longitude: Double): Boolean {
    return any { it.contains(latitude, longitude) }
}

private class ApiFetchError : Exception()

enum class WmtsEvent {
    OUT_OF_BOUNDS
}

sealed interface WmtsState
object Loading : WmtsState
data class MapReady(val mapState: MapState) : WmtsState
data class AreaSelection(val mapState: MapState, val areaUiController: AreaUiController) : WmtsState
enum class WmtsError : WmtsState {
    VPS_FAIL, IGN_OUTAGE, PROVIDER_OUTAGE
}

private fun WmtsState.getMapState(): MapState? {
    return when (this) {
        is AreaSelection -> mapState
        Loading -> null
        is MapReady -> mapState
        is WmtsError -> null
    }
}

private data class FormerProperties(val scale: Float, val scroll: Offset)
