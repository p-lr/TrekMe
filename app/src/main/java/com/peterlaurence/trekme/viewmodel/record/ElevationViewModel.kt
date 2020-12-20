package com.peterlaurence.trekme.viewmodel.record

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.repositories.recording.ElevationRepository
import com.peterlaurence.trekme.repositories.recording.GpxRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ElevationViewModel @ViewModelInject constructor(
        private val repository: ElevationRepository,
        private val gpxRepository: GpxRepository
) : ViewModel() {
    val elevationPoints = repository.elevationRepoState

    fun onUpdateGraph(targetWidth: Int) = viewModelScope.launch {
        gpxRepository.gpxForElevation.collect {
            repository.update(it, targetWidth)
        }
    }
}
