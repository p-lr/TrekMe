package com.peterlaurence.trekme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.service.LocationService
import com.peterlaurence.trekme.service.event.LocationServiceStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Expose to the activity and fragment/views the state of the [LocationService].
 * It listens the [LocationServiceStatus] event that the service sends through the event-bus.
 *
 * @author peterLaurence on 27/04/2019
 */
class LocationServiceViewModel: ViewModel() {
    private val status = MutableLiveData<Boolean>()

    fun getStatus(): LiveData<Boolean> {
        return status
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationServiceStatusEvent(event: LocationServiceStatus) {
        status.postValue(event.started)
    }

    init {
        EventBus.getDefault().register(this)

        status.postValue(LocationService.isStarted)
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}