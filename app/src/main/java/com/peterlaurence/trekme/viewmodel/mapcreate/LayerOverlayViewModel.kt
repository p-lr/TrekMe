package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.repositories.mapcreate.LayerOverlayRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Acts as a middle-man between the [LayerOverlayRepository] and the [LayerOverlayFragment].
 *
 * @author P.Laurence on 2021-01-11
 */
@HiltViewModel
class LayerOverlayViewModel @Inject constructor(
        private val repository: LayerOverlayRepository
) : ViewModel() {
    private val _liveData = MutableLiveData<List<LayerProperties>>()
    val liveData: LiveData<List<LayerProperties>> = _liveData

    fun init(wmtsSource: WmtsSource) {
        val data = repository.getLayerProperties(wmtsSource)
        _liveData.value = data
    }

    fun getAvailableLayersId(wmtsSource: WmtsSource): List<String> {
        return repository.getAvailableLayersId(wmtsSource)
    }

    fun addLayer(wmtsSource: WmtsSource, id: String) {
        val data = repository.addLayer(wmtsSource, id)
        _liveData.value = data
    }

    fun moveLayer(wmtsSource: WmtsSource, from: Int, to: Int) {
        val data = repository.moveLayer(wmtsSource, from, to) ?: return
        _liveData.value = data
    }

    fun removeLayer(wmtsSource: WmtsSource, index: Int) {
        val data = repository.removeLayer(wmtsSource, index) ?: return
        _liveData.value = data
    }
}