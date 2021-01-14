package com.peterlaurence.trekme.repositories.mapcreate

import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.ignLayersOverlay

class LayerOverlayRepository {
    private val model: MutableMap<WmtsSource, MutableList<LayerProperties>> = mutableMapOf()

    fun getLayerProperties(source: WmtsSource): List<LayerProperties> {
        return model.getOrPut(source) {
            mutableListOf()
        }
    }

    fun getAvailableLayersId(wmtsSource: WmtsSource): List<String> {
        return if (wmtsSource == WmtsSource.IGN) {
            ignLayersOverlay.map { it.id }
        } else listOf()
    }

    fun addLayer(wmtsSource: WmtsSource, id: String): List<LayerProperties> {
        when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = ignLayersOverlay.firstOrNull { it.id == id } ?: return listOf()
                val existingLayers = getLayerProperties(wmtsSource) as MutableList
                if (!existingLayers.any { it.layer.id == layer.id }) {
                    existingLayers.add(LayerProperties(layer, 0.5f))
                }
            }
            else -> {
            }
        }
        return getLayerProperties(wmtsSource)
    }
}

data class LayerProperties(val layer: Layer, var opacity: Float)