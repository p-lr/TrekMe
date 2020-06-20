package com.peterlaurence.trekme.viewmodel.mapcreate

import android.app.Application
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.core.mapsource.wmts.Point
import com.peterlaurence.trekme.core.mapsource.wmts.getMapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.providers.bitmap.checkIgnProvider
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.model.providers.stream.createTileStreamProvider
import com.peterlaurence.trekme.service.DownloadService
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * View-model for [GoogleMapWmtsViewFragment]. It takes care of:
 * * storing the predefined init scale and position for each [MapSource]
 * * keeping track of the layer (as to each [MapSource] may correspond multiple layers)
 * * providing a [TileStreamProvider] for the fragment
 *
 * @author peterLaurence on 09/11/19
 */
class GoogleMapWmtsViewModel @ViewModelInject constructor(
        private val settings: Settings,
        private val mapSourceCredentials: MapSourceCredentials
) : ViewModel() {
    private val scaleAndScrollInitConfig = mapOf(
            MapSource.SWISS_TOPO to ScaleAndScrollInitConfig(0.0006149545f, 21064, 13788),
            MapSource.IGN_SPAIN to ScaleAndScrollInitConfig(0.0003546317f, 11127, 8123)
    )

    private val activeLayerForSource = mutableMapOf<MapSource, Layer>()

    private suspend fun initIgnLayer(): IgnLayer = withContext(Dispatchers.IO) {
        /* Test the Scan Express Standard layer. If it's not working, return the classic layer */
        val tileStreamProvider = try {
            createTileStreamProvider(MapSource.IGN, ScanExpressStandard.realName, mapSourceCredentials)
        } catch (e: Exception) {
            return@withContext IgnClassic
        }
        if (checkIgnProvider(tileStreamProvider)) {
            ScanExpressStandard
        } else {
            IgnClassic
        }
    }

    fun getScaleAndScrollInitConfig(mapSource: MapSource): ScaleAndScrollInitConfig? {
        return scaleAndScrollInitConfig[mapSource]
    }

    fun getLayerPublicNameForSource(mapSource: MapSource): String {
        return activeLayerForSource[mapSource]?.publicName ?: ""
    }

    fun setLayerPublicNameForSource(mapSource: MapSource, layerName: String) {
        if (mapSource == MapSource.IGN) {
            try {
                val layer = listOf(ScanExpressStandard, IgnClassic, Satellite).first { it.publicName == layerName }
                activeLayerForSource[mapSource] = layer
            } catch (e: Exception) {
                /* Default value just in case */
                IgnClassic
            }
        }
    }

    suspend fun createTileStreamProvider(mapSource: MapSource): TileStreamProvider? {
        val layer = activeLayerForSource[mapSource] ?: initIgnLayer()
        return try {
            createTileStreamProvider(mapSource, layer.realName, mapSourceCredentials)
        } catch (e: Exception) {
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
    fun onDownloadFormConfirmed(application: Application, mapSource: MapSource,
                                p1: Point, p2: Point, minLevel: Int, maxLevel: Int) {
        val mapSpec = getMapSpec(minLevel, maxLevel, p1, p2)
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1, p2)
        viewModelScope.launch {
            val tileStreamProvider = createTileStreamProvider(mapSource)

            if (tileStreamProvider != null) {
                EventBus.getDefault().postSticky(RequestDownloadMapEvent(mapSource, mapSpec, tileCount, tileStreamProvider))
            }

            val intent = Intent(application, DownloadService::class.java)
            application.startService(intent)
        }
    }
}

data class ScaleAndScrollInitConfig(val scale: Float, val scrollX: Int, val scrollY: Int)