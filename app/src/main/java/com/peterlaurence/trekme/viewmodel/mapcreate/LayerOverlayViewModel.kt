package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.Road
import com.peterlaurence.trekme.core.providers.layers.Slopes
import com.peterlaurence.trekme.core.providers.layers.ignLayersOverlay

class LayerOverlayViewModel : ViewModel() {
    private val model: MutableMap<WmtsSource, List<LayerProperties>> = mutableMapOf()

    fun getSelectedLayers(wmtsSource: WmtsSource): List<LayerProperties> {
        return model[wmtsSource] ?: listOf()
    }

    fun getAvailableLayers(wmtsSource: WmtsSource): List<LayerInfo> {
        return if (wmtsSource == WmtsSource.IGN) {
            ignLayersOverlay.map { layer ->
                val name = when (layer) {
                    is Road -> "Roads"
                    is Slopes -> "Slopes"
                }
                LayerInfo(name, layer.id)
            }
        } else listOf()
    }

}

data class LayerProperties(val name: String, val layer: Layer, val opacity: Float)
data class LayerInfo(val name: String, val id: String)