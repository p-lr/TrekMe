package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.service.event.ChannelTrackPointRequest
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * This view-model backs the feature that enables a fragment to be aware of a recording
 * (e.g a list of [TrackPoint]) potentially launched way before **and** to be aware of new recording
 * points as they arrive. The use case is for example:
 * 1- User launches a recording directly from the MapViewFragment.
 * 2- The "live route" starts drawing on the screen
 * 3- For some reason the user leaves the fragment or even closes the app (without stopping the recording)
 * 4- User returns to his map and expects to see his "live route" still there.
 *
 * **Implementation**
 *
 * A producer-consumer pattern is employed here. The producer is the LocationService, the consumer
 * is a coroutine inside this view-model: [processNewTrackPoints]. Between the two, a [Channel]
 * serves as pipe and guaranties that no synchronisation issue will occur (despite the fact that
 * producer and the consumer work in two different threads).
 *
 * When this view-model first starts or even at further instantiations, it uses the event-bus to
 * request the channel from the producer.
 * Upon reception of the channel, it launches its consumer coroutine ([processNewTrackPoints]).
 */
class InMapRecordingViewModel : ViewModel() {
    private val route = MutableLiveData<RouteGson.Route>()

    init {
        EventBus.getDefault().register(this)
        requestTrackPointChannel()
    }

    private fun requestTrackPointChannel() {
        EventBus.getDefault().post(ChannelTrackPointRequest())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTrackPointChannelReceive(event: Channel<TrackPoint>) {
        viewModelScope.processNewTrackPoints(event)
    }

    /**
     * The coroutine that takes a [Channel] of [TrackPoint] as input and process new points as they
     * arrive.
     */
    private fun CoroutineScope.processNewTrackPoints(trackPoints: ReceiveChannel<TrackPoint>) =
            launch {
                for (point in trackPoints) {
                    processSinglePoint(point)
                }
            }

    private fun processSinglePoint(point: TrackPoint) {
        println("Processing point ${point.latitude}")
        // TODO: implement
    }

    fun getLiveRoute(): LiveData<RouteGson.Route> {
        return route
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}