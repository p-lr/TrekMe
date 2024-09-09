package com.peterlaurence.trekme.features.common.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.contains
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapSelectionDialogViewModel @Inject constructor(
    private val mapRepository: MapRepository
): ViewModel() {
    private val _mapList = MutableStateFlow<List<Map>>(emptyList())
    val mapList = _mapList.asStateFlow()

    fun init(boundingBox: BoundingBox?) = viewModelScope.launch {
        _mapList.value = if (boundingBox != null) {
            mapRepository.getCurrentMapList().filter { it.contains(boundingBox) }
        } else {
            mapRepository.getCurrentMapList()
        }
    }
}