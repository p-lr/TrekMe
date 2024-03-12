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
        WmtsSource.entries.associateWith {
            MutableStateFlow(emptyList())
        }

    /**
     * Due to how the layer overlay UI works at the moment, the value for a given StateFlow might
     * change excessively rapidly (when changing order).
     * Consumers should cancel flow collection when the map isn't visible.
     */
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

    fun moveLayerUp(wmtsSource: WmtsSource, id: String) {
        layersForSource[wmtsSource]?.update {
            val index = it.indexOfFirst { p -> p.layer.id == id }
            if (index > 0) {
                val newList = it.toList()
                Collections.swap(newList, index, index - 1)
                newList
            } else it
        }
    }

    fun moveLayerDown(wmtsSource: WmtsSource, id: String) {
        layersForSource[wmtsSource]?.update {
            val index = it.indexOfFirst { p -> p.layer.id == id }
            if (index < it.lastIndex) {
                val newList = it.toList()
                Collections.swap(newList, index, index + 1)
                newList
            } else it
        }
    }

    fun removeLayer(wmtsSource: WmtsSource, id: String) {
        layersForSource[wmtsSource]?.update {
            val newList = it.toMutableList()
            val index = it.indexOfFirst { p -> p.layer.id == id }
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
