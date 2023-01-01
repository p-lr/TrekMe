package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TracksManageViewModel2 @Inject constructor(
    private val mapRepository: MapRepository,
    private val routeInteractor: RouteInteractor
) : ViewModel() {

    fun getRouteFlow(): StateFlow<List<Route>> {
        return mapRepository.getCurrentMap()?.routes ?: MutableStateFlow(emptyList())
    }

    fun onColorChange(route: Route, color: Long) = viewModelScope.launch {
        val map = mapRepository.getCurrentMap() ?: return@launch
        routeInteractor.setRouteColor(map, route, color)
    }

    fun toggleRouteVisibility(route: Route) = viewModelScope.launch {
        val map = mapRepository.getCurrentMap() ?: return@launch
        routeInteractor.toggleRouteVisibility(map, route)
    }
}

