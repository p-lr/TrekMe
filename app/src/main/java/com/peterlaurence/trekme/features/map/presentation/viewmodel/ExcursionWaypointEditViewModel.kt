package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.ExcursionWaypointEditArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcursionWaypointEditViewModel @Inject constructor(
    private val excursionInteractor: ExcursionInteractor,
    private val excursionRepository: ExcursionRepository,
    mapRepository: MapRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = ExcursionWaypointEditArgs(savedStateHandle)
    private val excursionId = args.excursionId
    private val waypointId = args.waypointId

    val waypointState by lazy {
        getWayPointState()
    }

    val defaultColorState = mapRepository.getCurrentMap()?.excursionRefs?.value?.firstOrNull {
        it.id == excursionId
    }?.color ?: MutableStateFlow(null)


    fun saveWaypoint(lat: Double?, lon: Double?, name: String, comment: String, color: String?) {
        viewModelScope.launch {
            val waypoint = waypointState.value ?: return@launch
            excursionInteractor.updateAndSaveWaypoint(
                excursionId,
                waypoint,
                name,
                lat,
                lon,
                comment,
                color
            )
        }
    }

    private fun getWayPointState(): StateFlow<ExcursionWaypoint?> {
        return channelFlow {
            excursionRepository.getWaypoints(excursionId)?.collect { wpts ->
                val wpt = wpts.firstOrNull { it.id == waypointId }
                if (wpt != null) send(wpt)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
}