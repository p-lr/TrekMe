package com.peterlaurence.trekme.viewmodel.mapcreate

import android.app.Application
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.IgnSourceData
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.NoData
import com.peterlaurence.trekme.core.mapsource.OrdnanceSurveyData
import com.peterlaurence.trekme.core.mapsource.wmts.Point
import com.peterlaurence.trekme.core.mapsource.wmts.getMapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.providers.layers.IgnClassic
import com.peterlaurence.trekme.core.providers.layers.IgnLayer
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.ignLayers
import com.peterlaurence.trekme.model.providers.stream.createTileStreamProvider
import com.peterlaurence.trekme.service.DownloadService
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import com.peterlaurence.trekme.ui.mapcreate.views.GoogleMapWmtsViewFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.net.URL

/**
 * View-model for [GoogleMapWmtsViewFragment]. It takes care of:
 * * storing the predefined init scale and position for each [MapSource]
 * * keeping track of the layer (as to each [MapSource] may correspond multiple layers)
 * * providing a [TileStreamProvider] for the fragment
 *
 * @author peterLaurence on 09/11/19
 */
class GoogleMapWmtsViewModel @ViewModelInject constructor(
        private val app: Application
) : ViewModel() {
    private val defaultIgnLayer: IgnLayer = IgnClassic
    private var ignApi: String? = null
    private var ordnanceSurveyApi: String? = null
    private val ignApiUrl = "https://plrapps.ovh:8080/ign-api"
    private val ordnanceSurveyApiUrl = "https://plrapps.ovh:8080/ordnance-survey-api"

    private val scaleAndScrollInitConfig = mapOf(
            MapSource.IGN to listOf(
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
            MapSource.OPEN_STREET_MAP to listOf(
                    BoundariesConfig(listOf(
                            BoundingBox(-80.0, 83.0, -180.0, 180.0)        // World
                    ))
            ),
            MapSource.USGS to listOf(
                    ScaleLimitsConfig(maxScale = 0.25f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 16),
                    BoundariesConfig(listOf(
                            BoundingBox(24.69, 49.44, -124.68, -66.5)
                    ))
            ),
            MapSource.SWISS_TOPO to listOf(
                    InitScaleAndScrollConfig(0.0006149545f, 21064, 13788),
                    ScaleLimitsConfig(minScale = 0.0006149545f, maxScale = 0.5f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 17),
                    BoundariesConfig(listOf(
                            BoundingBox(45.78, 47.838, 5.98, 10.61)
                    ))
            ),
            MapSource.IGN_SPAIN to listOf(
                    InitScaleAndScrollConfig(0.0003546317f, 11127, 8123),
                    ScaleLimitsConfig(minScale = 0.0003546317f, maxScale = 0.5f),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    LevelLimitsConfig(levelMax = 17),
                    BoundariesConfig(listOf(
                            BoundingBox(35.78, 43.81, -9.55, 3.32)
                    ))
            ),
            MapSource.ORDNANCE_SURVEY to listOf(InitScaleAndScrollConfig(0.000830759f, 27011, 17261),
                    ScaleLimitsConfig(minScale = 0.000830759f, maxScale = 0.25f),
                    LevelLimitsConfig(7, 16),
                    ScaleForZoomOnPositionConfig(scale = 0.125f),
                    BoundariesConfig(listOf(
                            BoundingBox(49.8, 61.08, -8.32, 2.04)
                    ))
            )
    )

    private val activeLayerForSource: MutableMap<MapSource, Layer> = mutableMapOf(
            MapSource.IGN to defaultIgnLayer
    )

    fun getScaleAndScrollConfig(mapSource: MapSource): List<Config>? {
        return scaleAndScrollInitConfig[mapSource]
    }

    fun getLayerPublicNameForSource(mapSource: MapSource): String {
        return activeLayerForSource[mapSource]?.publicName ?: ""
    }

    fun setLayerPublicNameForSource(mapSource: MapSource, layerName: String) {
        if (mapSource == MapSource.IGN) {
            try {
                val layer = ignLayers.first { it.publicName == layerName }
                activeLayerForSource[mapSource] = layer
            } catch (e: Exception) {
                /* Default value just in case */
                defaultIgnLayer
            }
        }
    }

    suspend fun createTileStreamProvider(mapSource: MapSource): TileStreamProvider? {
        val mapSourceData = when (mapSource) {
            MapSource.IGN -> {
                val layer = activeLayerForSource[mapSource] ?: defaultIgnLayer
                if (ignApi == null) {
                    ignApi = getApi(ignApiUrl)
                }
                IgnSourceData(ignApi ?: "", layer)
            }
            MapSource.ORDNANCE_SURVEY -> {
                if (ordnanceSurveyApi == null) {
                    ordnanceSurveyApi = getApi(ordnanceSurveyApiUrl)
                }
                OrdnanceSurveyData(ordnanceSurveyApi ?: "")
            }
            else -> NoData
        }
        return try {
            createTileStreamProvider(mapSource, mapSourceData)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getApi(urlStr: String): String? = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val connection = url.openConnection()
        try {
            connection.getInputStream().bufferedReader().use {
                it.readText()
            }
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * We start the download with the [DownloadService]. A sticky event is posted right before
     * the service is started, which the service will read when it's started.
     *
     * WmtsLevelsDialog                            DownloadService
     *                                sticky
     *      RequestDownloadMapEvent   ----->          (event available)
     *      Intent                    ----->          (service start, then process event)
     */
    fun onDownloadFormConfirmed(mapSource: MapSource,
                                p1: Point, p2: Point, minLevel: Int, maxLevel: Int) {
        val mapSpec = getMapSpec(minLevel, maxLevel, p1, p2)
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1, p2)
        viewModelScope.launch {
            val tileStreamProvider = createTileStreamProvider(mapSource)

            if (tileStreamProvider != null) {
                EventBus.getDefault().postSticky(RequestDownloadMapEvent(mapSource, mapSpec, tileCount, tileStreamProvider))
            }

            val intent = Intent(app, DownloadService::class.java)
            app.startService(intent)
        }
    }
}

sealed class Config
data class InitScaleAndScrollConfig(val scale: Float, val scrollX: Int, val scrollY: Int) : Config()
data class ScaleForZoomOnPositionConfig(val scale: Float) : Config()
data class ScaleLimitsConfig(val minScale: Float? = null, val maxScale: Float? = null) : Config()
data class LevelLimitsConfig(val levelMin: Int = 1, val levelMax: Int = 18) : Config()
data class BoundariesConfig(val boundingBoxList: List<BoundingBox>) : Config()


data class BoundingBox(val latitudeMin: Double = 0.0, val latitudeMax: Double = 0.0,
                       val longitudeMin: Double = 0.0, val longitudeMax: Double = 0.0)

fun List<BoundingBox>.contains(latitude: Double, longitude: Double): Boolean {
    return any { it.contains(latitude, longitude) }
}

fun BoundingBox.contains(latitude: Double, longitude: Double): Boolean {
    return latitude in latitudeMin..latitudeMax && longitude in longitudeMin..longitudeMax
}