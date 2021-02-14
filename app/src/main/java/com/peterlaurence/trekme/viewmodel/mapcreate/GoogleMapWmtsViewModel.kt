package com.peterlaurence.trekme.viewmodel.mapcreate

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.backendApi.ordnanceSurveyApiUrl
import com.peterlaurence.trekme.core.map.BoundingBox
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.map.contains
import com.peterlaurence.trekme.core.mapsource.*
import com.peterlaurence.trekme.core.mapsource.wmts.Point
import com.peterlaurence.trekme.core.mapsource.wmts.getMapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.core.providers.stream.TileStreamProviderOverlay
import com.peterlaurence.trekme.core.providers.stream.createTileStreamProvider
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.ign.IgnApiRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerOverlayRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties
import com.peterlaurence.trekme.service.DownloadService
import com.peterlaurence.trekme.service.event.DownloadMapRequest
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.GoogleMapWmtsViewFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private val layerOverlayRepository: LayerOverlayRepository
) : ViewModel() {
    private val defaultIgnLayer: IgnLayer = IgnClassic
    private val defaultOsmLayer: OsmLayer = WorldStreetMap
    private var ordnanceSurveyApi: String? = null

    private val _wmtsSourceAccessibility = MutableStateFlow(true)
    val wmtsSourceAccessibility = _wmtsSourceAccessibility.asStateFlow()

    private val scaleAndScrollInitConfig = mapOf(
            WmtsSource.IGN to listOf(
                    ScaleLimitsConfig(maxScale = 0.5f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 17),
                    BoundariesConfig(listOf(
                            BoundingBox(41.21, 51.05, -4.92, 8.37),        // France
                            BoundingBox(-21.39, -20.86, 55.20, 55.84),     // La Réunion
                            BoundingBox(2.07, 5.82, -54.66, -51.53),       // Guyane
                            BoundingBox(15.82, 16.54, -61.88, -60.95),     // Guadeloupe
                            BoundingBox(18.0, 18.135, -63.162, -62.965),   // St Martin
                            BoundingBox(17.856, 17.988, -62.957, -62.778), // St Barthélemy
                            BoundingBox(14.35, 14.93, -61.31, -60.75),     // Martinique
                            BoundingBox(-17.945, -17.46, -149.97, -149.1), // Tahiti
                    ))),
            WmtsSource.OPEN_STREET_MAP to listOf(
                    BoundariesConfig(listOf(
                            BoundingBox(-80.0, 83.0, -180.0, 180.0)        // World
                    ))
            ),
            WmtsSource.USGS to listOf(
                    ScaleLimitsConfig(maxScale = 0.25f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 16),
                    BoundariesConfig(listOf(
                            BoundingBox(24.69, 49.44, -124.68, -66.5)
                    ))
            ),
            WmtsSource.SWISS_TOPO to listOf(
                    InitScaleAndScrollConfig(0.0006149545f, 21064, 13788),
                    ScaleLimitsConfig(minScale = 0.0006149545f, maxScale = 0.5f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 17),
                    BoundariesConfig(listOf(
                            BoundingBox(45.78, 47.838, 5.98, 10.61)
                    ))
            ),
            WmtsSource.IGN_SPAIN to listOf(
                    InitScaleAndScrollConfig(0.0003546317f, 11127, 8123),
                    ScaleLimitsConfig(minScale = 0.0003546317f, maxScale = 0.5f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 17),
                    BoundariesConfig(listOf(
                            BoundingBox(35.78, 43.81, -9.55, 3.32)
                    ))
            ),
            WmtsSource.ORDNANCE_SURVEY to listOf(InitScaleAndScrollConfig(0.000830759f, 27011, 17261),
                    ScaleLimitsConfig(minScale = 0.000830759f, maxScale = 0.25f),
                    LevelLimitsConfig(7, 16),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    BoundariesConfig(listOf(
                            BoundingBox(49.8, 61.08, -8.32, 2.04)
                    ))
            )
    )

    private val activeLayerForSource: MutableMap<WmtsSource, Layer> = mutableMapOf(
            WmtsSource.IGN to defaultIgnLayer
    )

    fun getScaleAndScrollConfig(wmtsSource: WmtsSource): List<Config>? {
        return scaleAndScrollInitConfig[wmtsSource]
    }

    fun getAvailablePrimaryLayersForSource(wmtsSource: WmtsSource): List<Layer>? {
        return when (wmtsSource) {
            WmtsSource.IGN -> ignLayersPrimary
            WmtsSource.OPEN_STREET_MAP -> osmLayers
            else -> null
        }
    }

    /**
     * Returns the active primary layer for the given source.
     */
    fun getPrimaryLayerForSource(wmtsSource: WmtsSource): Layer? {
        return when (wmtsSource) {
            WmtsSource.IGN -> activeLayerForSource[wmtsSource] ?: defaultIgnLayer
            WmtsSource.OPEN_STREET_MAP -> activeLayerForSource[wmtsSource] ?: defaultOsmLayer
            else -> null
        }
    }

    fun setLayerForSourceFromId(wmtsSource: WmtsSource, layerId: String) {
        val layer = getLayer(layerId)
        if (layer != null) {
            activeLayerForSource[wmtsSource] = layer
        }
    }

    /**
     * Returns the active overlay layers for the given source.
     */
    fun getOverlayLayersForSource(wmtsSource: WmtsSource): List<LayerProperties> {
        return layerOverlayRepository.getLayerProperties(wmtsSource)
    }

    suspend fun createTileStreamProvider(wmtsSource: WmtsSource): TileStreamProvider? {
        val mapSourceData = when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = getPrimaryLayerForSource(wmtsSource)!!
                val overlays = getOverlayLayersForSource(wmtsSource)
                val ignApi = ignApiRepository.getApi()
                IgnSourceData(ignApi ?: "", layer, overlays)
            }
            WmtsSource.ORDNANCE_SURVEY -> {
                if (ordnanceSurveyApi == null) {
                    ordnanceSurveyApi = getApi(ordnanceSurveyApiUrl)
                }
                OrdnanceSurveyData(ordnanceSurveyApi ?: "")
            }
            WmtsSource.OPEN_STREET_MAP -> {
                val layer = getPrimaryLayerForSource(wmtsSource)!!
                OsmSourceData(layer)
            }
            else -> NoData
        }
        return try {
            createTileStreamProvider(wmtsSource, mapSourceData).also {
                /* Don't test the stream provider if it has overlays */
                if (it !is TileStreamProviderOverlay) {
                    checkTileAccessibility(wmtsSource, it)
                }
            }
        } catch (e: Exception) {
            null
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

    /**
     * We start the download with the [DownloadService]. The download request is posted to the
     * relevant repository before the service is started.
     * The service then process the request when it starts.
     */
    fun onDownloadFormConfirmed(wmtsSource: WmtsSource,
                                p1: Point, p2: Point, minLevel: Int, maxLevel: Int) {
        val mapSpec = getMapSpec(minLevel, maxLevel, p1, p2)
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1, p2)
        viewModelScope.launch {
            val tileStreamProvider = createTileStreamProvider(wmtsSource) ?: return@launch
            val layer = getPrimaryLayerForSource(wmtsSource)
            val request = DownloadMapRequest(wmtsSource, layer, mapSpec, tileCount, tileStreamProvider)
            downloadRepository.postDownloadMapRequest(request)
            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
    }

    /**
     * Simple check whether we are able to download tiles or not.
     */
    private fun checkTileAccessibility(wmtsSource: WmtsSource, tileStreamProvider: TileStreamProvider) {
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
}

sealed class Config
data class InitScaleAndScrollConfig(val scale: Float, val scrollX: Int, val scrollY: Int) : Config()
data class ScaleForZoomOnPositionConfig(val scale: Float) : Config()
data class ScaleLimitsConfig(val minScale: Float? = null, val maxScale: Float? = null) : Config()
data class LevelLimitsConfig(val levelMin: Int = 1, val levelMax: Int = 18) : Config()
data class BoundariesConfig(val boundingBoxList: List<BoundingBox>) : Config()

fun List<BoundingBox>.contains(latitude: Double, longitude: Double): Boolean {
    return any { it.contains(latitude, longitude) }
}
