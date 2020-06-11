package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.track.toMarker
import com.peterlaurence.trekme.model.map.MapModel
import com.peterlaurence.trekme.service.event.ChannelTrackPointRequest
import com.peterlaurence.trekme.service.event.LocationServiceStatus
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * This view-model backs the feature that enables a fragment to be aware of a recording
 * (e.g a list of [TrackPoint]) potentially launched way before **and** to be aware of new recording
 * points as they arrive. The use case is for example:
 * 1- User opens one map (MapViewFragment)
 * 2- User launches a recording for the RecordingFragment.
 * 3- The "live route" starts drawing on the screen (MapViewFragment)
 * 4- For some reason the user leaves the MapViewFragment or even closes the app (without stopping
 *    the recording)
 * 5- User returns to his map and expects to see his "live route" still there.
 *
 * Actually it should work for any map: the "live route" can be drawn for any map while a recording
 * is running.
 *
 * **Implementation**
 *
 * A producer-consumer pattern is employed here. The producer is the LocationService, the consumer
 * is a coroutine inside this view-model: [processNewTrackPoints]. Between the two, a [Channel]
 * serves as pipe and guaranties that no synchronisation issue will occur (despite the fact that
 * producer and the consumer work in two different threads).
 *
 * When this view-model first starts or upon request from the fragment (when the map changes), it
 * uses the event-bus to request the channel from the producer.
 * Upon reception of the channel, it launches its consumer coroutine ([processNewTrackPoints]).
 * The [MapModel] is used to fetch the current map.
 */
class InMapRecordingViewModel : ViewModel() {
    private val route = MutableLiveData<LiveRoute>()

    init {
        EventBus.getDefault().register(this)
        requestTrackPointChannel()
    }

    fun reload() {
        requestTrackPointChannel()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onLocationServiceEvent(event: LocationServiceStatus) {
        if (event.started) {
            requestTrackPointChannel()
        }
    }

    private fun requestTrackPointChannel() {
        EventBus.getDefault().post(ChannelTrackPointRequest())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTrackPointChannelReceive(channel: Channel<TrackPoint>) {
        val map = MapModel.getCurrentMap()
        if (map != null) {
            viewModelScope.processNewTrackPoints(channel, map)
        }
    }

    /**
     * The coroutine that takes a [Channel] of [TrackPoint] as input and process new points as they
     * arrive.
     */
    private fun CoroutineScope.processNewTrackPoints(trackPoints: ReceiveChannel<TrackPoint>, map: Map) =
            launch(Dispatchers.Default) {
                val routeBuilder =  RouteBuilder(map)
                for (point in trackPoints) {
                    processSinglePoint(point, routeBuilder)
                }
            }

    /**
     * The [RouteBuilder] is being added new [TrackPoint] as they arrive.
     * Right after a point has been added, the [route] is updated. While this is done off UI thread,
     * the [MutableLiveData] will trigger observers in the UI thread. In other words,
     * [MutableLiveData] is thread safe.
     */
    private fun processSinglePoint(point: TrackPoint, routeBuilder: RouteBuilder) {
        routeBuilder.add(point)
        route.postValue(routeBuilder.liveRoute)
    }

    fun getLiveRoute(): LiveData<LiveRoute> {
        return route
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }

    class RouteBuilder(val map: Map) {
        val liveRoute = LiveRoute()

        fun add(point: TrackPoint) {
            val marker = point.toMarker(map)
            liveRoute.route_markers.add(marker)
        }
    }
}

/* A LiveRoute is just one particular route */
typealias LiveRoute = RouteGson.Route