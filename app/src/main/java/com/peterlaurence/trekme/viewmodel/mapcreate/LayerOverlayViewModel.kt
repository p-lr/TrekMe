package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.Road
import com.peterlaurence.trekme.core.providers.layers.Slopes
import com.peterlaurence.trekme.core.providers.layers.ignLayersOverlay

class LayerOverlayViewModel : ViewModel() {
    private val model: MutableMap<WmtsSource, MutableList<LayerProperties>> = mutableMapOf()

    fun getSelectedLayers(wmtsSource: WmtsSource): List<LayerProperties> {
        return model[wmtsSource] ?: listOf()
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
                val existingLayers = model.getOrPut(wmtsSource) {
                    mutableListOf()
                }
                if (!existingLayers.any { it.layer.id == id }) {
                    existingLayers.add(LayerProperties(layer, 0.5f))
                }
            }
            else -> {}
        }
    }
}

data class LayerProperties(val layer: Layer, var opacity: Float)