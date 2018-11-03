package com.peterlaurence.trekadvisor.model

import com.peterlaurence.trekadvisor.core.mapsource.MapSource
import com.peterlaurence.trekadvisor.core.providers.layers.IgnLayers

/**
 * This singleton holds the business logic of holding the association between a [MapSource] and a
 * layer, during the lifetime of the application.
 *
 * This is used when creating and downloading a map.
 *
 * @author perterLaurence on 03/11/18
 */
object LayerForSource {
    /**
     * Default layers are set there.
     */
    private val layerForSource = mutableMapOf(
            MapSource.IGN to IgnLayers.ScanExpressStandard.publicName
    )

    fun getLayerPublicNameForSource(mapSource: MapSource): String {
        return layerForSource[mapSource] ?: ""
    }

    fun setLayerPublicNameForSource(mapSource: MapSource, layerName: String) {
        layerForSource[mapSource] = layerName
    }

    fun resolveLayerName(mapSource: MapSource): String {
        val layerPublicName = getLayerPublicNameForSource(mapSource)
        return resolveLayerName(mapSource, layerPublicName)
    }

    private fun resolveLayerName(mapSource: MapSource, layerPublicName: String): String {
        return when (mapSource) {
            MapSource.IGN -> {
                try {
                    IgnLayers.values().first { it.publicName == layerPublicName }.realName
                } catch (e: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }
}