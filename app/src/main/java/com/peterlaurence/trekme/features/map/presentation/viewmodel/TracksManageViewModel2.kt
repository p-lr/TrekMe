package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TracksManageViewModel2 @Inject constructor(
    private val mapRepository: MapRepository
) : ViewModel() {

    fun getRouteFlow(): StateFlow<List<Route>> {
        return mapRepository.getCurrentMap()?.routes ?: MutableStateFlow(emptyList())
    }
}

