package com.peterlaurence.trekme.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.repositories.recording.GpxRecordRepository
import com.peterlaurence.trekme.service.GpxRecordService

/**
 * Expose to the activity and fragment/views the state of the [GpxRecordService].
 *
 * @author P.Laurence on 27/04/2019
 */
class GpxRecordServiceViewModel @ViewModelInject constructor(
        gpxRecordRepository: GpxRecordRepository
) : ViewModel() {
    val status: LiveData<Boolean> = gpxRecordRepository.serviceState.asLiveData(viewModelScope.coroutineContext)
}