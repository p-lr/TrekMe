package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.peterlaurence.trekme.core.wmts.domain.tools.getMapSpec
import com.peterlaurence.trekme.core.wmts.domain.tools.getNumberOfTiles
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionMarker
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.DownloadFormData
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.WmtsFragment
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.AreaUiController
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components.PlaceMarker
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
import com.peterlaurence.trekme.features.mapcreate.domain.interactors.Wgs84ToNormalizedInteractor
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toDomain
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model.toModel
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
    private val getTileStreamProviderDao: TileStreamProviderDao,
    private val checkTileStreamProviderDao: CheckTileStreamProviderDao,
    private val wmtsSourceRepository: WmtsSourceRepository,
    private val layerOverlayRepository: LayerOverlayRepository,
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

    val wmtsSourceState = wmtsSourceRepository.wmtsSourceState
    val locationFlow = locationSource.locationFlow

    val eventListState = mutableStateListOf<WmtsEvent>()

    private val defaultIgnLayer: IgnLayer = IgnClassic
    private val defaultOsmLayer: OsmLayer = WorldStreetMap

    private val areaController = AreaUiController()
    private var downloadFormData: DownloadFormData? = null

    private val searchFieldState: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    private var hasPrimaryLayers = false
    private var hasOverlayLayers = false
    private var hasExtendedOffer = false

    private val routeLayer = RouteLayer(viewModelScope, wgs84ToNormalizedInteractor)
    private val geoRecordUrls = mutableSetOf<Uri>()

    private val activePrimaryLayerForSource: MutableMap<WmtsSource, Layer> = mutableMapOf(
        WmtsSource.IGN to defaultIgnLayer
    )

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

        val mapState = MapState(
            19, mapSize, mapSize,
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
        val mapConfiguration = getScaleAndScrollConfig(wmtsSource)
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
            mapState.scale = previousMapState.scale
            launch {
                mapState.setScroll(previousMapState.scroll)
            }
            /* Restore the location of the place marker (not to confuse with the position marker) */
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
            hasTrackImport = hasExtendedOffer
        )

        /* Restore the location marker right now - even if subsequent updates will do it anyway. */
        updatePositionOneTime()

        val tileStreamProviderFlow = createTileStreamProvider(wmtsSource)
        tileStreamProviderFlow.collect { result ->
            val tileStreamProvider = result.getOrNull()
            if (tileStreamProvider != null) {
                mapState.removeAllLayers()
                mapState.addLayer(tileStreamProvider.toMapComposeTileStreamProvider())
            } else {
                _wmtsState.value = WmtsError.VPS_FAIL
            }
        }
    }

    private fun getScaleAndScrollConfig(wmtsSource: WmtsSource): List<Config> {
        return when (wmtsSource) {
            WmtsSource.IGN -> ignConfig
            WmtsSource.SWISS_TOPO -> swissTopoConfig
            WmtsSource.OPEN_STREET_MAP -> osmConfig
            WmtsSource.USGS -> usgsConfig
            WmtsSource.IGN_SPAIN -> ignSpainConfig
            WmtsSource.ORDNANCE_SURVEY -> ordnanceSurveyConfig
        }
    }

    private fun updateTopBarConfig(wmtsSource: WmtsSource) {
        hasExtendedOffer = extendedOfferStateOwner.purchaseFlow.value == PurchaseState.PURCHASED
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

    private fun getActivePrimaryIgnLayer(): IgnLayer {
        return activePrimaryLayerForSource[WmtsSource.IGN] as? IgnLayer ?: defaultIgnLayer
    }

    private fun getActivePrimaryOsmLayer(): OsmLayer {
        return activePrimaryLayerForSource[WmtsSource.OPEN_STREET_MAP] as? OsmLayer ?: defaultOsmLayer
    }

    fun onPrimaryLayerDefined(layerId: String) = viewModelScope.launch {
        val wmtsSource = wmtsSourceRepository.wmtsSourceState.value ?: return@launch
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
            else -> null
        }
    }

    private suspend fun createTileStreamProvider(wmtsSource: WmtsSource): Flow<Result<TileStreamProvider>> {
        val mapSourceData: Flow<MapSourceData> = when (wmtsSource) {
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
            WmtsSource.USGS -> flow { emit(UsgsData) }
            WmtsSource.IGN_SPAIN -> flow { emit(IgnSpainData) }
        }

        return mapSourceData.map {
            getTileStreamProviderDao.newTileStreamProvider(it).also { result ->
                /* Don't test the stream provider if it has overlays */
                val provider = result.getOrNull()
                if (provider != null && provider !is TileStreamProviderOverlay) {
                    checkTileAccessibility(wmtsSource, provider)
                }
            }
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
        val levelConf = if (wmtsSource == WmtsSource.OPEN_STREET_MAP && hasExtendedOffer) {
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

        val mapSourceBundle = if (levelConf != null) {
            if (startMaxLevel != null) {
                DownloadFormData(
                    wmtsSource,
                    p1,
                    p2,
                    levelConf.levelMin,
                    levelConf.levelMax,
                    startMaxLevel
                )
            } else {
                DownloadFormData(wmtsSource, p1, p2, levelConf.levelMin, levelConf.levelMax)
            }
        } else {
            DownloadFormData(wmtsSource, p1, p2)
        }

        return mapSourceBundle.also {
            downloadFormData = it
        }
    }

    /**
     * Start the download with the [DownloadService]. The download request is posted to the
     * relevant repository before the service is started.
     * The service then process the request when it starts.
     */
    fun onDownloadFormConfirmed(minLevel: Int, maxLevel: Int) {
        val downloadForm = downloadFormData ?: return
        val (wmtsSource, p1, p2) = downloadForm

        val mapSpec = getMapSpec(minLevel, maxLevel, p1.toDomain(), p2.toDomain())
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1.toDomain(), p2.toDomain())
        viewModelScope.launch {
            val tileStreamProvider = createTileStreamProvider(
                wmtsSource
            ).firstOrNull()?.getOrNull() ?: return@launch
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
            val boundaryConf = mapConfiguration.filterIsInstance<BoundariesConfig>().firstOrNull()
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
            hasTrackImport = hasExtendedOffer
        )
    }
}

private const val positionMarkerId = "position"
private const val placeMarkerId = "place"

fun List<BoundingBox>.contains(latitude: Double, longitude: Double): Boolean {
    return any { it.contains(latitude, longitude) }
}

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
