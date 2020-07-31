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
            MapSource.SWISS_TOPO to ScaleAndScrollConfig(0.0006149545f, 21064, 13788, 0.0006149545f),
            MapSource.IGN_SPAIN to ScaleAndScrollConfig(0.0003546317f, 11127, 8123, 0003546317f),
            MapSource.ORDNANCE_SURVEY to ScaleAndScrollConfig(0.000830759f, 27011, 17261, 0.000830759f)
    )

    private val activeLayerForSource: MutableMap<MapSource, Layer> = mutableMapOf(
            MapSource.IGN to defaultIgnLayer
    )

    fun getScaleAndScrollConfig(mapSource: MapSource): ScaleAndScrollConfig? {
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

data class ScaleAndScrollConfig(val scale: Float, val scrollX: Int, val scrollY: Int,
                                val minScale: Float? = null)