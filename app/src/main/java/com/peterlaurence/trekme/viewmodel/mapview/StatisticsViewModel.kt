package com.peterlaurence.trekme.viewmodel.mapview

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.repositories.recording.GpxRecordRepository
import com.peterlaurence.trekme.service.GpxRecordService
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * The view-model for displaying track statistics in the MapView fragment.
 *
 * @author P.Laurence on 01/05/20
 */
class StatisticsViewModel @ViewModelInject constructor(
        private val gpxRecordRepository: GpxRecordRepository
) : ViewModel() {
    /* In this context, a null value means that statistics shouldn't be displayed - the view should
     * reflect this appropriately */
    private val _stats = MutableLiveData<TrackStatistics?>()
    val stats: LiveData<TrackStatistics?> = _stats

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTrackStatistics(event: TrackStatistics) {
        _stats.value = event
    }

    /**
     * When the [GpxRecordService] emits a [GpxFileWriteEvent] (which means that it stopped recording
     * and has now written data to a gpx file), this view-model might or might not still be registered
     * as listener. If it's still registered, then we publish a null value. If not, then it means
     * that the next time the user navigates to the map view, a new instance of this view-model will
     * be created - so by construction [_stats] will hold a null value.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGpxFileWriteEvent(event: GpxFileWriteEvent) {
        _stats.value = null
    }

    init {
        EventBus.getDefault().register(this)

        if (!gpxRecordRepository.serviceState.value) {
            _stats.value = null
        }
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
        super.onCleared()
    }
}