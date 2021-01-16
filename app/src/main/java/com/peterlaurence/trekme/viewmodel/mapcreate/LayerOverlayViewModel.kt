package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.repositories.mapcreate.LayerOverlayRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties

class LayerOverlayViewModel @ViewModelInject constructor(
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
        val data = repository.moveLayer(wmtsSource, from, to)
        _liveData.value = data
    }

    fun removeLayer(wmtsSource: WmtsSource, index: Int) {
        val data = repository.removeLayer(wmtsSource, index)
        _liveData.value = data
    }
}