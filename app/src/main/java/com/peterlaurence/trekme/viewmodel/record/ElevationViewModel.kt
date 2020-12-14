package com.peterlaurence.trekme.viewmodel.record

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.repositories.recording.ElevationRepository

class ElevationViewModel @ViewModelInject constructor(
        private val repository: ElevationRepository
) : ViewModel() {
    val elevationPoints = repository.elevationPoints

    fun onUpdateGraph(targetWidth: Int) {
        repository.update(targetWidth)
    }
}
