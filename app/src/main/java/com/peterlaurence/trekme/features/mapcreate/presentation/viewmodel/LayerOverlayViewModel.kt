package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.domain.repository.LayerOverlayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * The view-model for the view showing the list of possible overlay layers.
 *
 * @since 2021-01-11
 */
@HiltViewModel
class LayerOverlayViewModel @Inject constructor(
    private val repository: LayerOverlayRepository,
) : ViewModel() {

    fun getLayerPropertiesFlow(wmtsSource: WmtsSource): StateFlow<List<LayerProperties>> {
        return repository.getLayerProperties(wmtsSource)
    }

    fun updateOpacityForLayer(wmtsSource: WmtsSource, layerId: String, opacity: Float) {
        repository.updateOpacityForLayer(wmtsSource, layerId, opacity)
    }

    fun getAvailableLayersId(wmtsSource: WmtsSource): List<String> {
        return repository.getAvailableLayersId(wmtsSource)
    }

    fun addLayer(wmtsSource: WmtsSource, id: String) {
        repository.addLayer(wmtsSource, id)
    }

    fun moveLayer(wmtsSource: WmtsSource, from: Int, to: Int) {
        repository.moveLayer(wmtsSource, from, to)
    }

    fun removeLayer(wmtsSource: WmtsSource, index: Int) {
        repository.removeLayer(wmtsSource, index)
    }
}