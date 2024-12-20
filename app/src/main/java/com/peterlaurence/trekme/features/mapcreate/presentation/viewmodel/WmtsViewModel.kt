package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.di.TrekmeExtended
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderOverlay
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.core.geocoding.domain.repository.GeocodingRepository
import com.peterlaurence.trekme.core.map.domain.dao.CheckTileStreamProviderDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.model.*
import com.peterlaurence.trekme.features.mapcreate.domain.repository.LayerOverlayRepository
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionMarker
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.DownloadFormData
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.AreaUiController
import com.peterlaurence.trekme.features.common.presentation.ui.component.PlaceMarker
import com.peterlaurence.trekme.features.common.domain.util.toMapComposeTileStreamProvider
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.BoundariesConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.Config
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.InitScaleAndScrollConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.LevelLimitsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ScaleForZoomOnPositionConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ScaleLimitsConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignSpainConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ordnanceSurveyConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.osmConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.swissTopoConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.usgsConfig
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.ParseGeoRecordInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToMercatorInteractor
import com.peterlaurence.trekme.core.wmts.domain.tools.getNumberOfTiles
import com.peterlaurence.trekme.core.wmts.domain.tools.getOptimizedMinLevel
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.ignBelgiumConfig
import com.peterlaurence.trekme.features.common.presentation.ui.mapcompose.osmHdConfig
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toDomain
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toModel
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.layers.RouteLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.layout.Fit
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

/**
 * View-model for the map creation screen. It takes care of:
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
    private val getTileStreamProviderDao: TileStreamProviderDao,
    private val checkTileStreamProviderDao: CheckTileStreamProviderDao,
    private val wmtsSourceRepository: WmtsSourceRepository,
    private val layerOverlayRepository: LayerOverlayRepository,
    private val locationSource: LocationSource,
    private val geocodingRepository: GeocodingRepository,
    private val parseGeoRecordInteractor: ParseGeoRecordInteractor,
    private val wgs84ToMercatorInteractor: Wgs84ToMercatorInteractor,
    @IGN
    private val extendedOfferWithIgnStateOwner: ExtendedOfferStateOwner,
    @TrekmeExtended
    private val extendedOfferStateOwner: ExtendedOfferStateOwner
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(Wmts(Loading))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _topBarState = MutableStateFlow<TopBarState>(Empty)
    val topBarState: StateFlow<TopBarState> = _topBarState.asStateFlow()

    private val _wmtsState = MutableStateFlow<WmtsState>(Loading)

    val wmtsSourceState = wmtsSourceRepository.wmtsSourceState
    val locationFlow = locationSource.locationFlow

    /* Allow the "view" to collect those events only at some lifecycle stages, in order to avoid
     * unnecessary network requests when e.g changing overlay layers from a different screen. */
    private val _tileStreamProviderChannel = Channel<TileStreamProvider>(1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val tileStreamProviderFlow = _tileStreamProviderChannel.receiveAsFlow()

    private val _eventsChannel = Channel<WmtsEvent>(1)
    val events = _eventsChannel.receiveAsFlow()

    private val defaultIgnLayer = IgnClassic
    private val defaultOsmLayer = WorldStreetMap
    private val defaultUsgsLayer = UsgsTopo

    private val areaController = AreaUiController()
    private var downloadFormData: DownloadFormData? = null

    private var hasPrimaryLayers = false
    private var hasOverlayLayers = false

    val hasExtendedOffer = combine(extendedOfferWithIgnStateOwner.purchaseFlow, extendedOfferStateOwner.purchaseFlow) { x, y ->
        x == PurchaseState.PURCHASED || y == PurchaseState.PURCHASED
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val routeLayer = RouteLayer(viewModelScope, wgs84ToMercatorInteractor)
    private val geoRecordUrls = mutableSetOf<Uri>()

    private val activePrimaryLayerForSource = mutableMapOf<WmtsSource, Layer>()

    /* While awaiting the first location, and if the user hasn't panned or zoomed the map, let this
     * flag to true. */
    private var shouldCenterOnNextLocation = true

    private var lastSearch: String? = null
    private var lastPlaces: List<GeoPlace>? = null

    val isSearchPendingState = geocodingRepository.isLoadingFlow

    init {
        viewModelScope.launch {
            wmtsSourceRepository.wmtsSourceState.collectLatest { source ->
                source?.also { updateMapState(source, false) }
            }
        }

        geocodingRepository.geoPlaceFlow.map { places ->
            if (_uiState.value is GeoplaceList) {
                _uiState.value = GeoplaceList(places)
            }
            lastPlaces = places
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
                is Loading, is WmtsError -> null
                is MapReady -> {
                    areaController.attachAndInit(st.mapState)
                    AreaSelection(st.mapState, areaController)
                }
            } ?: return@launch

            _wmtsState.value = nextState
        }
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

        /* Depending on the max level, initialize the MapState with the right size and level count */
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val levelMaxConfig = mapConfiguration.firstNotNullOfOrNull {
            if (it is LevelLimitsConfig) it else null
        }

        val tileSize = getTileSize(wmtsSource)

        val (wmtsLevelMax, size) = if (levelMaxConfig != null) {
            val lvlMax = levelMaxConfig.levelMax
            Pair(lvlMax, mapSizeAtLevel(lvlMax, tileSize = tileSize))
        } else Pair(18, mapSizeAtLevel(18, tileSize = tileSize))

        val mapState = MapState(
            levelCount = wmtsLevelMax + 1, // wmts levels are 0-based
            size, size,
            tileSize = tileSize,
            workerCount = 16
        ) {
            magnifyingFactor(
                if (wmtsSource == WmtsSource.OPEN_STREET_MAP) 1 else 0
            )
        }.apply {
            disableFlingZoom()
            /* Use grey background to contrast with the material 3 top app bar in light mode */
            setMapBackground(Color(0xFFF8F8F8))
        }

        /* Apply configuration */
        mapConfiguration.forEach { conf ->
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
                else -> {} /* Nothing to do */
            }
        }

        /* Apply former settings, if any */
        if (restorePrevious && previousMapState != null) {
            launch {
                mapState.scrollTo(previousMapState.centroidX, previousMapState.centroidY, destScale = previousMapState.scale)
            }
            /* Restore the location of the place marker (not to confuse with the position marker) */
            previousMapState.getMarkerInfo(placeMarkerId)?.also { markerInfo ->
                updatePlacePosition(mapState, markerInfo.x, markerInfo.y)
            }
        }

        mapState.onFirstMove(this) {
            shouldCenterOnNextLocation = false
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
            hasTrackImport = hasExtendedOffer.value
        )

        /* Restore the location marker right now - even if subsequent updates will do it anyway. */
        updatePositionOneTime()

        val tileStreamProviderFlow = createTileStreamProvider(wmtsSource)
        tileStreamProviderFlow.collect { (_, result) ->
            val tileStreamProvider = result.getOrNull()
            if (tileStreamProvider != null) {
                _tileStreamProviderChannel.send(tileStreamProvider)
            } else {
                _wmtsState.value = WmtsError.VPS_FAIL
            }
        }
    }

    /**
     * Executes [block] action once, when the map was detected idle for the first time and after
     * the first move.
     */
    private fun MapState.onFirstMove(scope: CoroutineScope, block: () -> Unit) {
        scope.launch {
            var idleOnce = false
            idleStateFlow().collect { idle ->
                if (idle) {
                    idleOnce = true
                } else {
                    if (idleOnce) {
                        block()
                        cancel()
                    }
                }
            }
        }
    }

    fun onNewTileStreamProvider(tileStreamProvider: TileStreamProvider) {
        val mapState = _wmtsState.value.let { if (it is MapReady) it.mapState else null} ?: return

        mapState.removeAllLayers()
        mapState.addLayer(
            /* The retry policy is applied here to affect map display only, and to not
             * cumulate with the retry policy already applied when downloading maps. */
            tileStreamProvider.withRetry(3).toMapComposeTileStreamProvider()
        )
    }

    private fun getTileSize(wmtsSource: WmtsSource): Int {
        return if (wmtsSource == WmtsSource.OPEN_STREET_MAP) {
            when (getActivePrimaryOsmLayer()) {
                OsmAndHd, Outdoors -> 512
                else -> 256
            }
        } else 256
    }

    private fun getScaleAndScrollConfig(wmtsSource: WmtsSource): List<Config> {
        return when (wmtsSource) {
            WmtsSource.IGN -> ignConfig
            WmtsSource.SWISS_TOPO -> swissTopoConfig
            WmtsSource.OPEN_STREET_MAP -> {
                when (getActivePrimaryOsmLayer()) {
                    OsmAndHd, Outdoors -> osmHdConfig
                    else -> osmConfig
                }
            }
            WmtsSource.USGS -> usgsConfig
            WmtsSource.IGN_SPAIN -> ignSpainConfig
            WmtsSource.ORDNANCE_SURVEY -> ordnanceSurveyConfig
            WmtsSource.IGN_BE -> ignBelgiumConfig
        }
    }

    private fun updateTopBarConfig(wmtsSource: WmtsSource) {
        hasPrimaryLayers = when (wmtsSource) {
            WmtsSource.IGN, WmtsSource.OPEN_STREET_MAP, WmtsSource.USGS -> true
            else -> false
        }
        hasOverlayLayers = wmtsSource == WmtsSource.IGN
    }

    fun getAvailablePrimaryLayersForSource(wmtsSource: WmtsSource): List<Layer>? {
        return when (wmtsSource) {
            WmtsSource.IGN -> ignLayersPrimary
            WmtsSource.OPEN_STREET_MAP -> osmLayersPrimary.let {
                /* If extended offer, osm hd first */
                if (hasExtendedOffer.value) {
                    it.sortedByDescending { l -> l == OsmAndHd }
                } else it
            }
            WmtsSource.USGS -> usgsLayersPrimary
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
            WmtsSource.USGS -> getActivePrimaryUsgsLayer()
            else -> null
        }
    }

    private fun getActivePrimaryIgnLayer(): IgnPrimaryLayer {
        return activePrimaryLayerForSource[WmtsSource.IGN] as? IgnPrimaryLayer ?: defaultIgnLayer
    }

    private fun getActivePrimaryOsmLayer(): OsmPrimaryLayer {
        return activePrimaryLayerForSource[WmtsSource.OPEN_STREET_MAP] as? OsmPrimaryLayer ?: run {
            if (hasExtendedOffer.value) OsmAndHd else defaultOsmLayer
        }
    }

    private fun getActivePrimaryUsgsLayer(): UsgsPrimaryLayer {
        return activePrimaryLayerForSource[WmtsSource.USGS] as? UsgsPrimaryLayer ?: defaultUsgsLayer
    }

    fun onPrimaryLayerDefined(layerId: String) = viewModelScope.launch {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return@launch

        if (wmtsSource == WmtsSource.OPEN_STREET_MAP) {
            if (layerId == osmAndHd && !hasExtendedOffer.value) {
                _eventsChannel.send(WmtsEvent.SHOW_TREKME_EXTENDED_ADVERT)
                return@launch
            }
        }
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

    private fun getPrimaryLayer(id: String): Layer? {
        return when (id) {
            ignPlanv2 -> PlanIgnV2
            ignClassic -> IgnClassic
            ignSatellite -> Satellite
            osmTopo -> WorldTopoMap
            osmStreet -> WorldStreetMap
            openTopoMap -> OpenTopoMap
            cyclOSM -> CyclOSM
            osmAndHd -> OsmAndHd
            usgsTopo -> UsgsTopo
            usgsImageryTopo -> UsgsImageryTopo
            else -> null
        }
    }

    private suspend fun createTileStreamProvider(wmtsSource: WmtsSource): Flow<Pair<MapSourceData, Result<TileStreamProvider>>> {
        val mapSourceDataFlow: Flow<MapSourceData> = getMapSourceDataFlow(wmtsSource)

        return mapSourceDataFlow.map {
            Pair(it, getTileStreamProviderDao.newTileStreamProvider(it)).also { (_, result) ->
                /* Don't test the stream provider if it has overlays */
                val provider = result.getOrNull()
                if (provider != null && provider !is TileStreamProviderOverlay) {
                    checkTileAccessibility(wmtsSource, provider)
                }
            }
        }
    }

    private suspend fun getMapSourceDataFlow(wmtsSource: WmtsSource): Flow<MapSourceData> {
        return when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = getActivePrimaryIgnLayer()
                getOverlayLayersForSource(wmtsSource).map { overlays ->
                    val overlaysIgn = overlays.filterIsInstance<LayerPropertiesIgn>()
                    IgnSourceData(layer, overlaysIgn)
                }
            }
            WmtsSource.ORDNANCE_SURVEY -> {
                flow { emit(OrdnanceSurveyData) }
            }
            WmtsSource.OPEN_STREET_MAP -> {
                val layer = getActivePrimaryOsmLayer()
                flow { emit(OsmSourceData(layer)) }
            }
            WmtsSource.SWISS_TOPO -> flow { emit(SwissTopoData) }
            WmtsSource.USGS -> {
                val layer = getActivePrimaryUsgsLayer()
                flow { emit(UsgsData(layer)) }
            }
            WmtsSource.IGN_SPAIN -> flow { emit(IgnSpainData) }
            WmtsSource.IGN_BE -> flow { emit(IgnBelgiumData) }
        }
    }

    fun onValidateArea(): DownloadFormData? {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return null
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)

        /* Specifically for Cadastre IGN layer, set the startMaxLevel to 17 */
        val hasCadastreOverlay = getOverlayLayersForSource(wmtsSource).value.any { layerProp ->
            layerProp.layer is Cadastre
        }
        val startMaxLevel = if (hasCadastreOverlay) 17 else null

        /* Otherwise, honor the level limits configuration for this source, if any.
         * Make an exception for OSM in the case the user has the extended offer. */
        val levelConf = if (wmtsSource == WmtsSource.OPEN_STREET_MAP && hasExtendedOffer.value) {
            LevelLimitsConfig(levelMax = 17)
        } else {
            mapConfiguration.firstOrNull { conf -> conf is LevelLimitsConfig } as? LevelLimitsConfig
        }

        /* At this point, the current state should be AreaSelection */
        val areaSelectionState = _wmtsState.value as? AreaSelection ?: return null

        fun interpolate(t: Double, min: Double, max: Double) = min + t * (max - min)
        val p1 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p1x, X0, X1),
                interpolate(p1y, Y0, Y1)
            ).toModel()
        }
        val p2 = with(areaSelectionState.areaUiController) {
            Point(
                interpolate(p2x, X0, X1),
                interpolate(p2y, Y0, Y1)
            ).toModel()
        }

        val tilesNumberLimit = if (hasExtendedOffer.value) null else tileNumberLimit

        val levelMinOpt = getOptimizedMinLevel(p1.toDomain(), p2.toDomain())
        val mapSourceBundle = if (levelConf != null) {
            DownloadFormData(
                wmtsSource = wmtsSource,
                p1 = p1,
                p2 = p2,
                levelMin = levelConf.levelMin,
                levelMax = levelConf.levelMax,
                startMinLevel = levelMinOpt ?: 14,
                startMaxLevel = startMaxLevel ?: 16,
                tilesNumberLimit = tilesNumberLimit
            )
        } else {
            DownloadFormData(wmtsSource, p1, p2, tilesNumberLimit = tilesNumberLimit)
        }

        downloadFormData = mapSourceBundle
        return mapSourceBundle
    }

    /**
     * Start the download with the [DownloadService]. The download request is posted to the
     * relevant repository before the service is started.
     * The service then process the request when it starts.
     */
    fun onDownloadFormConfirmed(minLevel: Int, maxLevel: Int): Boolean {
        val downloadForm = downloadFormData ?: return true // download not launched, but ui should proceed as if it was.
        val (wmtsSource, p1, p2) = downloadForm

        /* If there's a limit on the number of tiles.
         * If purchase was made meanwhile, don't do this. */
        if (downloadForm.tilesNumberLimit != null && !hasExtendedOffer.value) {
            /* ..double-check the number of tiles */
            val tilesNumber = computeTilesNumber(minLevel, maxLevel, p1.toDomain(), p2.toDomain())
            if (tilesNumber > tileNumberLimit) {
                viewModelScope.launch {
                    _eventsChannel.send(WmtsEvent.SHOW_MAP_SIZE_LIMIT_RATIONALE)
                }
                /* Notify caller that download was blocked */
                return false
            }
        }

        val tileSize = getTileSize(wmtsSource)
        viewModelScope.launch {
            val mapSourceData = getMapSourceDataFlow(wmtsSource).firstOrNull() ?: return@launch
            val downloadSpec = NewDownloadSpec(
                corner1 = p1.toDomain(),
                corner2 = p2.toDomain(),
                minLevel = minLevel,
                maxLevel = maxLevel,
                tileSize = tileSize,
                source = mapSourceData,
                geoRecordUris = geoRecordUrls
            )
            downloadRepository.postMapDownloadSpec(downloadSpec)
            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
        return true
    }

    fun computeTilesNumber(minLevel: Int, maxLevel: Int, p1: Point, p2: Point): Long {
        return getNumberOfTiles(
            levelMin = minLevel,
            levelMax = maxLevel,
            point1 = p1,
            point2 = p2
        )
    }

    /**
     * Simple check whether we are able to download tiles or not.
     */
    private fun checkTileAccessibility(
        wmtsSource: WmtsSource,
        tileStreamProvider: TileStreamProvider
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = checkTileStreamProviderDao.check(wmtsSource, tileStreamProvider)
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
                wgs84ToMercatorInteractor.getNormalized(location.latitude, location.longitude)
            }

            /* Update the position */
            if (normalized != null) {
                updatePosition(mapState, normalized.x, normalized.y)
            }

            if (shouldCenterOnNextLocation) {
                shouldCenterOnNextLocation = false
                tryCenterOnPosition(location)
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
        viewModelScope.launch {
            val cachedLocation = locationSource.locationFlow.replayCache.firstOrNull()
            if (cachedLocation == null) {
                _eventsChannel.send(WmtsEvent.AWAITING_LOCATION)
                shouldCenterOnNextLocation = true
            } else {
                tryCenterOnPosition(cachedLocation)
            }
        }
    }

    private suspend fun tryCenterOnPosition(location: Location) {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val boundaryConf = mapConfiguration.filterIsInstance<BoundariesConfig>().firstOrNull()
        boundaryConf?.boundingBoxList?.also { boxes ->
            if (boxes.contains(location.latitude, location.longitude)) {
                centerOnPosition()
            } else {
                _eventsChannel.send(WmtsEvent.CURRENT_LOCATION_OUT_OF_BOUNDS)
            }
        }
    }

    private fun centerOnPosition() {
        val mapState = _wmtsState.value.getMapState() ?: return
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return

        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
        val scaleConf =
            mapConfiguration.filterIsInstance<ScaleForZoomOnPositionConfig>().firstOrNull()

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
        val boundaryConf = mapConfiguration.filterIsInstance<BoundariesConfig>().firstOrNull()
        boundaryConf?.boundingBoxList?.also { boxes ->
            if (!boxes.contains(place.lat, place.lon)) {
                viewModelScope.launch {
                    _eventsChannel.send(WmtsEvent.PLACE_OUT_OF_BOUNDS)
                }
                return
            }
        }

        /* If it's in the bounds, add a marker */
        val normalized = wgs84ToMercatorInteractor.getNormalized(place.lat, place.lon) ?: return
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
        _topBarState.value = SearchMode(lastSearch.orEmpty())
        _uiState.value = GeoplaceList(lastPlaces ?: emptyList())
    }

    fun onQueryTextSubmit(query: String) {
        if (query.isNotEmpty()) {
            geocodingRepository.search(query)
            lastSearch = query
        }
    }

    fun onCloseSearch() {
        /* Go back to map view */
        _uiState.value = Wmts(_wmtsState.value)

        /* Collapse the top bar */
        _topBarState.value = Collapsed(
            hasPrimaryLayers = hasPrimaryLayers,
            hasOverlayLayers = hasOverlayLayers,
            hasTrackImport = hasExtendedOffer.value
        )
    }
}

private const val positionMarkerId = "position"
private const val placeMarkerId = "place"

fun List<BoundingBox>.contains(latitude: Double, longitude: Double): Boolean {
    return any { it.contains(latitude, longitude) }
}

enum class WmtsEvent {
    CURRENT_LOCATION_OUT_OF_BOUNDS,
    PLACE_OUT_OF_BOUNDS,
    AWAITING_LOCATION,
    SHOW_TREKME_EXTENDED_ADVERT,
    SHOW_MAP_SIZE_LIMIT_RATIONALE
}

sealed interface UiState
data class Wmts(val wmtsState: WmtsState) : UiState
data class GeoplaceList(val geoPlaceList: List<GeoPlace>) : UiState

sealed interface WmtsState
data object Loading : WmtsState
data class MapReady(val mapState: MapState) : WmtsState
data class AreaSelection(val mapState: MapState, val areaUiController: AreaUiController) : WmtsState
enum class WmtsError : WmtsState {
    VPS_FAIL, IGN_OUTAGE, PROVIDER_OUTAGE
}

private fun WmtsState.getMapState(): MapState? {
    return when (this) {
        is AreaSelection -> mapState
        is Loading, is WmtsError -> null
        is MapReady -> mapState
    }
}

sealed interface TopBarState
data object Empty : TopBarState
data class Collapsed(val hasPrimaryLayers: Boolean, val hasOverlayLayers: Boolean, val hasTrackImport: Boolean) : TopBarState
data class SearchMode(val lastSearch: String) : TopBarState
