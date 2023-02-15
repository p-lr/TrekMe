package com.peterlaurence.trekme.features.record.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.map.domain.models.Map
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MapSelectionDialogViewModel @Inject constructor(
    private val mapRepository: MapRepository
): ViewModel() {
    fun getMapList(): List<Map> = mapRepository.getCurrentMapList()
}