package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.wmts.Point
import com.peterlaurence.trekme.core.mapsource.wmts.getMapSpec
import com.peterlaurence.trekme.core.mapsource.wmts.getNumberOfTiles
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.model.providers.stream.createTileStreamProvider
import com.peterlaurence.trekme.service.event.RequestDownloadMapEvent
import org.greenrobot.eventbus.EventBus

/**
 * View-model for [GoogleMapWmtsViewFragment]. It takes care of:
 * * keeping track of the layer (as to each [MapSource] may correspond multiple layers)
 * * providing a [TileStreamProvider] for the fragment
 *
 * @author peterLaurence on 09/11/19
 */
class GoogleMapWmtsViewModel : ViewModel() {
    private val layerForSource = mutableMapOf(
            MapSource.IGN to IgnLayers.ScanExpressStandard.publicName
    )

    fun getLayerPublicNameForSource(mapSource: MapSource): String {
        return layerForSource[mapSource] ?: ""
    }

    fun setLayerPublicNameForSource(mapSource: MapSource, layerName: String) {
        layerForSource[mapSource] = layerName
    }

    fun createTileStreamProvider(mapSource: MapSource): TileStreamProvider? {
        val layer = resolveLayerName(mapSource)
        return try {
            createTileStreamProvider(mapSource, layer)
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveLayerName(mapSource: MapSource): String {
        val layerPublicName = getLayerPublicNameForSource(mapSource)
        return resolveLayerName(mapSource, layerPublicName)
    }

    private fun resolveLayerName(mapSource: MapSource, layerPublicName: String): String {
        return when (mapSource) {
            MapSource.IGN -> {
                try {
                    IgnLayers.values().first { it.publicName == layerPublicName }.realName
                } catch (e: Exception) {
                    /* Default value just in case */
                    IgnLayers.ScanExpressStandard.realName
                }
            }
            else -> ""
        }
    }

    /**
     * When the user confirms a map download, prepare here the the necessary inputs for the service
     * which will take care of the download. BTW, this service is launched from the view, not this
     * view-model, we shall not have any reference to Activity context.
     */
    fun onDownloadFormConfirmed(mapSource: MapSource, p1: Point, p2: Point, minLevel: Int, maxLevel: Int) {
        val mapSpec = getMapSpec(minLevel, maxLevel, p1, p2)
        val tileCount = getNumberOfTiles(minLevel, maxLevel, p1, p2)
        val tileStreamProvider = createTileStreamProvider(mapSource)

        if (tileStreamProvider != null) {
            EventBus.getDefault().postSticky(RequestDownloadMapEvent(mapSource, mapSpec, tileCount, tileStreamProvider))
        }
    }
}