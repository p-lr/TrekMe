package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.ExcursionWaypointEditArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcursionWaypointEditViewModel @Inject constructor(
    private val excursionInteractor: ExcursionInteractor,
    private val excursionRepository: ExcursionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = ExcursionWaypointEditArgs(savedStateHandle)
    private val excursionId = args.excursionId
    private val waypointId = args.waypointId

    private val _waypointState = MutableStateFlow<ExcursionWaypoint?>(null)
    val waypointState = _waypointState.asStateFlow()

    init {
        viewModelScope.launch {
            _waypointState.value = getWaypoint()
        }
    }

    fun saveWaypoint(lat: Double?, lon: Double?, name: String, comment: String) =
        viewModelScope.launch {
            val waypoint = _waypointState.value ?: return@launch
            excursionInteractor.updateAndSaveWaypoint(
                excursionId,
                waypoint,
                name,
                lat,
                lon,
                comment
            )
        }

    private suspend fun getWaypoint(): ExcursionWaypoint? {
        return excursionRepository.getWaypoints(excursionId)?.value?.firstOrNull { it.id == waypointId }
    }
}