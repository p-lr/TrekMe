package com.peterlaurence.trekme.core.repositories.mapcreate

import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.ignLayersOverlay
import java.util.*

/**
 * Each [WmtsSource] can be associated with "overlay" layers, which should be drawn on top of the
 * primary layer.
 * This repository holds the correspondence between [WmtsSource]s and [LayerProperties], and exposes
 * methods to add, remove, and reorder layers.
 *
 * @author P.Laurence on 2021-01-14
 */
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
                    existingLayers.add(LayerProperties(layer, 1f))
                }
            }
            else -> {
            }
        }
        return getLayerProperties(wmtsSource)
    }

    fun moveLayer(wmtsSource: WmtsSource, from: Int, to: Int): List<LayerProperties>? {
        return model[wmtsSource]?.let {
            if (from in it.indices && to in it.indices) {
                Collections.swap(it, from, to)
                it
            } else null
        }
    }

    fun removeLayer(wmtsSource: WmtsSource, index: Int): List<LayerProperties>? {
        return model[wmtsSource]?.also {
            it.removeAt(index)
        }
    }
}

data class LayerProperties(val layer: Layer, var opacity: Float)