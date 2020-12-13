package com.peterlaurence.trekme.viewmodel.record

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.repositories.recording.ElevationRepository

class ElevationViewModel @ViewModelInject constructor(
        repository: ElevationRepository
) : ViewModel() {
    val elevationPoints = repository.elevationPoints

}
