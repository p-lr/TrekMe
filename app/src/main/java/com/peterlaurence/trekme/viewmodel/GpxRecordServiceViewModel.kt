package com.peterlaurence.trekme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.service.GpxRecordService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Expose to the activity and fragment/views the state of the [GpxRecordService].
 *
 * @author P.Laurence on 27/04/2019
 */
@HiltViewModel
class GpxRecordServiceViewModel @Inject constructor(
        gpxRecordEvents: GpxRecordEvents
) : ViewModel() {
    val status: LiveData<Boolean> = gpxRecordEvents.serviceState.asLiveData(viewModelScope.coroutineContext)
}