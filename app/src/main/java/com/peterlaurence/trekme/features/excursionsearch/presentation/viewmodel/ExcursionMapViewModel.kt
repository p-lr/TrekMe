package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.models.Map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource
): ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    private val _mapStateFlow = MutableStateFlow<UiState>(Loading)
    val mapStateFlow: StateFlow<UiState> = _mapStateFlow.asStateFlow()

}

sealed interface UiState
object Loading : UiState
data class MapReady(val mapState: MapState, val map: Map) : UiState
enum class Error: UiState {
    PROVIDER_OUTAGE
}