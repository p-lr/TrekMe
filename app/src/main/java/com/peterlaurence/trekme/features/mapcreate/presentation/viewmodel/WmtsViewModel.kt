package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.contains
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.models.DownloadMapRequest
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.stream.TileStreamProviderOverlay
import com.peterlaurence.trekme.core.providers.stream.newTileStreamProvider
import com.peterlaurence.trekme.core.repositories.api.IgnApiRepository
import com.peterlaurence.trekme.core.repositories.api.OrdnanceSurveyApiRepository
import com.peterlaurence.trekme.core.repositories.download.DownloadRepository
import com.peterlaurence.trekme.core.geocoding.domain.repository.GeocodingRepository
import com.peterlaurence.trekme.core.wmts.domain.model.*
import com.peterlaurence.trekme.features.mapcreate.domain.repository.LayerOverlayRepository
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.core.wmts.domain.tools.getMapSpec
import com.peterlaurence.trekme.core.wmts.domain.tools.getNumberOfTiles
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionMarker
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.DownloadFormDataBundle
import com.peterlaurence.trekme.features.mapcreate.presentation.events.MapCreateEventBus
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.WmtsFragment
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.AreaUiController
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.PlaceMarker
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.Point
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toDomain
import com.peterlaurence.trekme.features.common.domain.util.toMapComposeTileStreamProvider
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.ParseGeoRecordInteractor
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.layers.RouteLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

/**
 * View-model for [WmtsFragment]. It takes care of:
 * * storing the predefined init scale and position for each [WmtsSource]
 * * keeping track of the layer (as to each [WmtsSource] may correspond multiple layers)
 * * exposes [UiState] and [TopBarState] for the view made mostly in Compose
 *
 * @since 2019/11/09
 */
@HiltViewModel
class WmtsViewModel @Inject constructor(
    private val app: Application,
    private val downloadRepository: DownloadRepository,
    private val ignApiRepository: IgnApiRepository,
    private val wmtsSourceRepository: WmtsSourceRepository,
    private val ordnanceSurveyApiRepository: OrdnanceSurveyApiRepository,
    private val layerOverlayRepository: LayerOverlayRepository,
    private val mapCreateEventBus: MapCreateEventBus,
    private val locationSource: LocationSource,
    private val geocodingRepository: GeocodingRepository,
    private val parseGeoRecordInteractor: ParseGeoRecordInteractor,
    private val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor,
    private val extendedOfferStateOwner: ExtendedOfferStateOwner
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(Wmts(Loading))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState>(Empty)
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _wmtsState = MutableStateFlow<WmtsState>(Loading)
    val wmtsState: StateFlow<WmtsState> = _wmtsState.asStateFlow()

    val eventListState = mutableStateListOf<WmtsEvent>()

    private val defaultIgnLayer: IgnLayer = IgnClassic
    private val defaultOsmLayer: OsmLayer = WorldStreetMap

    private val areaController = AreaUiController()

    private val searchFieldState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    private var hasPrimaryLayers = false
    private var hasOverlayLayers = false
    private var hasTrackImport = false

    private val routeLayer = RouteLayer(viewModelScope, wgs84ToNormalizedInteractor)
    private val geoRecordUrls = mutableSetOf<Uri>()

    private val scaleAndScrollInitConfig = mapOf(
        WmtsSource.IGN to listOf(
            ScaleLimitsConfig(maxScale = 0.25f),
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 18),
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
            ScaleForZoomOnPositionConfig(scale = 0.125f),
            LevelLimitsConfig(levelMax = 16),
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

    init {
        viewModelScope.launch {
            wmtsSourceRepository.wmtsSourceState.collectLatest { source ->
                source?.also { updateMapState(source, false) }
            }
        }

        viewModelScope.launch {
            mapCreateEventBus.layerSelectEvent.collectLatest {
                onPrimaryLayerDefined(it)
            }
        }

        geocodingRepository.geoPlaceFlow.map { places ->
            if (places != null && _uiState.value is GeoplaceList) {
                _uiState.value = GeoplaceList(places)
            }
        }.launchIn(viewModelScope)

        _wmtsState.map {
            _uiState.value = Wmts(it)
        }.launchIn(viewModelScope)
    }

    fun toggleArea() {
        viewModelScope.launch {
            val nextState = when (val st = _wmtsState.value) {
                is AreaSelection -> {
                    areaController.detach(st.mapState)
                    MapReady(st.mapState)
                }
                Loading, is WmtsError -> null
                is MapReady -> {
                    areaController.attachAndInit(st.mapState)
                    AreaSelection(st.mapState, areaController)
                }
            } ?: return@launch

            _wmtsState.value = nextState
        }
    }

    fun acknowledgeError() {
        eventListState.removeFirstOrNull()
    }

    fun onTrackImport(uri: Uri) = viewModelScope.launch {
        geoRecordUrls.add(uri)
        parseGeoRecordInteractor.parseGeoRecord(uri, app.applicationContext.contentResolver)?.also {
            val mapState = _wmtsState.value.getMapState()
            if (mapState != null) {
                routeLayer.setGeoRecord(it, mapState)
            }
        }
    }

    /**
     * Builds the [MapState] and indefinitely suspends while listening for layers change.
     * Caller must cancel the parent coroutine on [WmtsSource] change.
     */
    private suspend fun updateMapState(wmtsSource: WmtsSource, restorePrevious: Boolean = true) = coroutineScope {
        val previousState = _wmtsState.value
        val previousMapState = previousState.getMapState()

        /* Shutdown the previous MapState, if any */
        previousMapState?.shutdown()

        /* Display the loading screen while building the new MapState */
        _wmtsState.value = Loading
        _topBarState.value = Empty

        val tileStreamProviderFlow = runCatching {
            createTileStreamProvider(wmtsSource)
        }.onFailure {
            _wmtsState.value = WmtsError.VPS_FAIL
        }.getOrNull() ?: return@coroutineScope

        val mapState = MapState(
            19, mapSize, mapSize,
            workerCount = 16
        ) {
            magnifyingFactor(
                if (wmtsSource == WmtsSource.OPEN_STREET_MAP) 1 else 0
            )
        }.apply {
            disableFlingZoom()
        }

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
        if (restorePrevious && previousMapState != null) {
            mapState.scale = previousMapState.scale
            launch {
                mapState.setScroll(previousMapState.scroll)
            }
            /* Restore the location of the place marker */
            previousMapState.getMarkerInfo(placeMarkerId)?.also { markerInfo ->
                updatePlacePosition(mapState, markerInfo.x, markerInfo.y)
            }
        }

        /* The top bar configuration depends on the wmtsSource */
        updateTopBarConfig(wmtsSource)

        /* If we were in area selection, restore it */
        _wmtsState.value = if (previousState is AreaSelection) {
            previousState.areaUiController.attach(mapState)
            AreaSelection(mapState, previousState.areaUiController)
        } else MapReady(mapState)

        _topBarState.value = Collapsed(
            hasPrimaryLayers = hasPrimaryLayers,
            hasOverlayLayers = hasOverlayLayers,
            hasTrackImport = hasTrackImport
        )

        /* Restore the location marker right now - even if subsequent updates will do it anyway. */
        updatePositionOneTime()

        tileStreamProviderFlow.collect { tileStreamProvider ->
            mapState.removeAllLayers()
            mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
        }
    }

    private fun getScaleAndScrollConfig(wmtsSource: WmtsSource): List<Config>? {
        return scaleAndScrollInitConfig[wmtsSource]
    }

    private fun updateTopBarConfig(wmtsSource: WmtsSource) {
        hasTrackImport = extendedOfferStateOwner.purchaseFlow.value == PurchaseState.PURCHASED
        hasPrimaryLayers = when (wmtsSource) {
            WmtsSource.IGN, WmtsSource.OPEN_STREET_MAP -> true
            else -> false
        }
        hasOverlayLayers = wmtsSource == WmtsSource.IGN
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

    private suspend fun onPrimaryLayerDefined(layerId: String) {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return
        setPrimaryLayerForSourceFromId(wmtsSource, layerId)

        updateMapState(wmtsSource, true)
    }

    private fun updatePositionOneTime() {
        locationSource.locationFlow.take(1).map {
            onLocationReceived(it)
        }.launchIn(viewModelScope)
    }

    private fun setPrimaryLayerForSourceFromId(wmtsSource: WmtsSource, layerId: String) {
        val layer = getPrimaryLayer(layerId)
        if (layer != null) {
            activePrimaryLayerForSource[wmtsSource] = layer
        }
    }

    /**
     * Returns the active overlay layers for the given source.
     */
    private fun getOverlayLayersForSource(wmtsSource: WmtsSource): StateFlow<List<LayerProperties>> {
        return layerOverlayRepository.getLayerProperties(wmtsSource)
    }

    /**
     * Creates the [TileStreamProvider] for the given source. If we couldn't fetch the API key (when
     * we should have been able to do so), an [IllegalStateException] is thrown.
     */
    @Throws(ApiFetchError::class)
    suspend fun createTileStreamProvider(wmtsSource: WmtsSource): Flow<TileStreamProvider> {
        val mapSourceData: Flow<MapSourceData> = when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = getActivePrimaryIgnLayer()
                getOverlayLayersForSource(wmtsSource).map { overlays ->
                    val ignApi = ignApiRepository.getApi() ?: throw ApiFetchError()
                    IgnSourceData(ignApi, layer, overlays)
                }
            }
            WmtsSource.ORDNANCE_SURVEY -> {
                val api = ordnanceSurveyApiRepository.getApi() ?: throw ApiFetchError()
                flow { emit(OrdnanceSurveyData(api)) }
            }
            WmtsSource.OPEN_STREET_MAP -> {
                val layer = getActivePrimaryOsmLayer()
                flow { emit(OsmSourceData(layer)) }
            }
            WmtsSource.SWISS_TOPO -> flow { emit(SwissTopoData) }
            WmtsSource.USGS -> flow { emit(UsgsData) }
            WmtsSource.IGN_SPAIN -> flow { emit(IgnSpainData) }
        }

        return mapSourceData.map {
            newTileStreamProvider(it).also { provider ->
                /* Don't test the stream provider if it has overlays */
                if (provider !is TileStreamProviderOverlay) {
                    checkTileAccessibility(wmtsSource, provider)
                }
            }
        }
    }

    fun onValidateArea() {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)

        /* Specifically for Cadastre IGN layer, set the startMaxLevel to 17 */
        val hasCadastreOverlay = getOverlayLayersForSource(wmtsSource).value.any { layerProp ->
            layerProp.layer is Cadastre
        }
        val startMaxLevel = if (hasCadastreOverlay) 17 else null

        /* Otherwise, honor the level limits configuration for this source, if any. */
        val levelConf =
            mapConfiguration?.firstOrNull { conf -> conf is LevelLimitsConfig } as? LevelLimitsConfig

        /* At this point, the current state should be AreaSelection */
        val areaSelectionState = _wmtsState.value as? AreaSelection ?: return

        fun interpolate(t: Double, min: Double, max: Double) = min + t * (max - min)
        val p1 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p1x, X0, X1),
                interpolate(p1y, Y0, Y1)
            )
        }
        val p2 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p2x, X0, X1),
                interpolate(p2y, Y0, Y1)
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
                createTileStreamProvider(wmtsSource).firstOrNull()
            }.getOrNull() ?: return@launch
            val request = DownloadMapRequest(wmtsSource, mapSpec, tileCount, tileStreamProvider, geoRecordUrls)
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
            val success = when (wmtsSource) {
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
            if (!success) {
                val errorState = if (wmtsSource == WmtsSource.IGN) {
                    WmtsError.IGN_OUTAGE
                } else {
                    WmtsError.PROVIDER_OUTAGE
                }
                _wmtsState.emit(errorState)
            }
        }
    }

    fun onLocationReceived(location: Location) {
        /* If there is no MapState, no need to go further */
        val mapState = _wmtsState.value.getMapState() ?: return

        viewModelScope.launch {
            /* Project lat/lon off UI thread */
            val normalized = withContext(Dispatchers.Default) {
                wgs84ToNormalizedInteractor.getNormalized(location.latitude, location.longitude)
            }

            /* Update the position */
            if (normalized != null) {
                updatePosition(mapState, normalized.x, normalized.y)
            }
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

    fun zoomOnPosition() {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        locationSource.locationFlow.take(1).map { location ->
            val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
            val boundaryConf = mapConfiguration?.filterIsInstance<BoundariesConfig>()?.firstOrNull()
            boundaryConf?.boundingBoxList?.also { boxes ->
                if (boxes.contains(location.latitude, location.longitude)) {
                    centerOnPosition()
                } else {
                    eventListState.add(WmtsEvent.CURRENT_LOCATION_OUT_OF_BOUNDS)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun centerOnPosition() {
        val mapState = _wmtsState.value.getMapState() ?: return
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val scaleConf =
            mapConfiguration?.filterIsInstance<ScaleForZoomOnPositionConfig>()?.firstOrNull()

        viewModelScope.launch {
            /* If we have conf, use it. Otherwise, use 1f - the scale is caped by the max scale anyway */
            mapState.centerOnMarker(positionMarkerId, scaleConf?.scale ?: 1f)
        }
    }

    fun moveToPlace(place: GeoPlace) {
        /* First, we check that this place is in the bounds of the map */
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        /* Collapse the top bar right now */
        onCloseSearch()

        val mapState = _wmtsState.value.getMapState() ?: return

        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val boundaryConf = mapConfiguration?.filterIsInstance<BoundariesConfig>()?.firstOrNull()
        boundaryConf?.boundingBoxList?.also { boxes ->
            if (!boxes.contains(place.lat, place.lon)) {
                eventListState.add(WmtsEvent.PLACE_OUT_OF_BOUNDS)
                return
            }
        }

        /* If it's in the bounds, add a marker */
        val normalized = wgs84ToNormalizedInteractor.getNormalized(place.lat, place.lon) ?: return
        updatePlacePosition(mapState, normalized.x, normalized.y)

        viewModelScope.launch {
            mapState.centerOnMarker(placeMarkerId, 0.25f)
        }
    }

    /**
     * Update the position of the place marker on the map. This method expects normalized coordinates.
     *
     * @param x the normalized x coordinate
     * @param y the normalized y coordinate
     */
    private fun updatePlacePosition(mapState: MapState, x: Double, y: Double) {
        if (mapState.hasMarker(placeMarkerId)) {
            mapState.moveMarker(placeMarkerId, x, y)
        } else {
            mapState.addMarker(placeMarkerId, x, y, relativeOffset = Offset(-0.5f, -1f)) {
                PlaceMarker()
            }
        }
    }

    fun onSearchClick() {
        _topBarState.value = SearchMode(searchFieldState)
        _uiState.value = GeoplaceList(listOf())
    }

    fun onQueryTextSubmit(query: String) {
        if (query.isNotEmpty()) {
            geocodingRepository.search(query)
        }
    }

    fun onCloseSearch() {
        /* Go back to map view */
        _uiState.value = Wmts(_wmtsState.value)

        /* Collapse the top bar */
        _topBarState.value = Collapsed(
            hasPrimaryLayers = hasPrimaryLayers,
            hasOverlayLayers = hasOverlayLayers,
            hasTrackImport = hasTrackImport
        )
    }
}

private const val positionMarkerId = "position"
private const val placeMarkerId = "place"

/* Size of level 18 (levels are 0-based) */
private const val mapSize = 67108864

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
    CURRENT_LOCATION_OUT_OF_BOUNDS, PLACE_OUT_OF_BOUNDS
}

sealed interface UiState
data class Wmts(val wmtsState: WmtsState) : UiState
data class GeoplaceList(val geoPlaceList: List<GeoPlace>) : UiState

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
        Loading, is WmtsError -> null
        is MapReady -> mapState
    }
}

sealed interface TopBarState
object Empty : TopBarState
data class Collapsed(val hasPrimaryLayers: Boolean, val hasOverlayLayers: Boolean, val hasTrackImport: Boolean) : TopBarState {
    val hasOverflowMenu: Boolean = hasOverlayLayers || hasTrackImport
}
data class SearchMode(val textValueState: MutableState<TextFieldValue>) : TopBarState
