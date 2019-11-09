package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.model.providers.stream.createTileStreamProvider

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
}