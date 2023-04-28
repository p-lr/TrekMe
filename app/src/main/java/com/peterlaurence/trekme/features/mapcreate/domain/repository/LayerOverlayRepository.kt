package com.peterlaurence.trekme.features.mapcreate.domain.repository

import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.core.wmts.domain.model.LayerPropertiesIgn
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.core.wmts.domain.model.ignLayersOverlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*

/**
 * Each [WmtsSource] can be associated with "overlay" layers, which should be drawn on top of the
 * primary layer.
 * This repository holds the correspondence between [WmtsSource]s and [LayerProperties], and exposes
 * methods to add, remove, and reorder layers.
 *
 * @since 2021-01-14
 */
class LayerOverlayRepository {
    private val layersForSource: Map<WmtsSource, MutableStateFlow<List<LayerProperties>>> =
        WmtsSource.values().associateWith {
            MutableStateFlow(emptyList())
        }

    fun getLayerProperties(source: WmtsSource): StateFlow<List<LayerProperties>> {
        return layersForSource[source]
            ?: MutableStateFlow(emptyList())  // the right-hand side should never happen
    }

    fun getAvailableLayersId(wmtsSource: WmtsSource): List<String> {
        return if (wmtsSource == WmtsSource.IGN) {
            ignLayersOverlay.map { it.id }
        } else listOf()
    }

    fun addLayer(wmtsSource: WmtsSource, id: String) {
        when (wmtsSource) {
            WmtsSource.IGN -> {
                val layer = ignLayersOverlay.firstOrNull { it.id == id } ?: return
                layersForSource[wmtsSource]?.update { existingLayers ->
                    if (!existingLayers.any { it.layer.id == layer.id }) {
                        existingLayers + LayerPropertiesIgn(layer, 1f)
                    } else existingLayers
                }
            }
            else -> {
            }
        }
    }

    fun moveLayer(wmtsSource: WmtsSource, from: Int, to: Int) {
        layersForSource[wmtsSource]?.update {
            if (from in it.indices && to in it.indices) {
                val newList = it.toList()
                Collections.swap(newList, from, to)
                newList
            } else it
        }
    }

    fun removeLayer(wmtsSource: WmtsSource, index: Int) {
        layersForSource[wmtsSource]?.update {
            val newList = it.toMutableList()
            newList.removeAt(index)
            newList
        }
    }

    fun updateOpacityForLayer(wmtsSource: WmtsSource, layerId: String, opacity: Float) {
        layersForSource[wmtsSource]?.update {
            it.mapNotNull { layerProperties ->
                if (layerProperties.layer.id == layerId) {
                    when (wmtsSource) {
                        WmtsSource.IGN -> {
                            (layerProperties as? LayerPropertiesIgn)?.copy(opacity = opacity)
                        }
                        else -> null
                    }
                } else layerProperties
            }
        }
    }
}
