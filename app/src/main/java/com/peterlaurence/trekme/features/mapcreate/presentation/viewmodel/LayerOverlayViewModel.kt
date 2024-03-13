package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.features.mapcreate.domain.repository.LayerOverlayRepository
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.LayerOverlayArg
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val arg = LayerOverlayArg(savedStateHandle)
    private val wmtsSource = arg.wmtsSource

    fun getLayerPropertiesFlow(): StateFlow<List<LayerProperties>> {
        return repository.getLayerProperties(wmtsSource)
    }

    fun updateOpacityForLayer(layerId: String, opacity: Float) {
        repository.updateOpacityForLayer(wmtsSource, layerId, opacity)
    }

    fun getAvailableLayersId(): List<String> {
        return repository.getAvailableLayersId(wmtsSource)
    }

    fun addLayer(id: String) {
        repository.addLayer(wmtsSource, id)
    }

    fun moveLayerUp(id: String) {
        repository.moveLayerUp(wmtsSource, id)
    }

    fun moveLayerDown(id: String) {
        repository.moveLayerDown(wmtsSource, id)
    }

    fun removeLayer(id: String) {
        repository.removeLayer(wmtsSource, id)
    }
}