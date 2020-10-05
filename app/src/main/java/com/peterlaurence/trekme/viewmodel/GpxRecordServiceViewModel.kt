package com.peterlaurence.trekme.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.event.GpxRecordServiceStatus
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

/**
 * Expose to the activity and fragment/views the state of the [GpxRecordService].
 * It listens the [GpxRecordServiceStatus] event that the service sends through the event-bus.
 *
 * TODO: Remove the dependency on EventBus from this class and only expose a LiveData from the
 * [GpxRecordService], or (maybe better) expose a Flow of [Boolean], to be converted to LiveData in this
 * ViewModel (using Flow.asLiveData()).
 *
 * @author P.Laurence on 27/04/2019
 */
class GpxRecordServiceViewModel : ViewModel() {
    private val statusFromEventBus = MutableLiveData(GpxRecordService.isStarted)
    val status: LiveData<Boolean> = statusFromEventBus

    @Subscribe
    fun onGpxRecordServiceStatusEvent(event: GpxRecordServiceStatus) {
        /* As the event might be coming from any thread, post on main thread */
        statusFromEventBus.postValue(event.started)
    }

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}