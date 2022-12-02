package com.peterlaurence.trekme.events.recording

import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.features.record.app.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.core.lib.gpx.model.TrackPoint
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

class GpxRecordEvents {
    private val _liveRouteFlow = MutableSharedFlow<LiveRouteEvent>(replay = Int.MAX_VALUE)
    val liveRouteFlow = _liveRouteFlow.asSharedFlow()

    fun addPointToLiveRoute(trackPoint: TrackPoint) {
        _liveRouteFlow.tryEmit(LiveRoutePoint(trackPoint))
    }

    fun pauseLiveRoute() {
        _liveRouteFlow.tryEmit(LiveRoutePause)
    }

    fun stopLiveRoute() {
        _liveRouteFlow.tryEmit(LiveRouteStop)
    }

    fun resetLiveRoute() {
        _liveRouteFlow.resetReplayCache()
        _liveRouteFlow.tryEmit(LiveRouteStop)
    }

    /**********************************************************************************************/

    /* Status started / stopped / paused of the service */
    private val _serviceState = MutableStateFlow(GpxRecordState.STOPPED)
    val serviceState : StateFlow<GpxRecordState> = _serviceState.asStateFlow()

    /**
     * Should only by used by the service.
     */
    fun setServiceState(state: GpxRecordState) {
        _serviceState.value = state
    }

    /**********************************************************************************************/

    /* The service listens to this signal. Upon reception of this signal, the service creates a
     * gpx file and stop itself after notifying its state. */
    private val _stopRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stopRecordingSignal = _stopRecordingSignal.asSharedFlow()

    fun stopRecording() {
        _stopRecordingSignal.tryEmit(Unit)
    }

    /**********************************************************************************************/

    /* The service listens to this signal. Upon reception of this signal, the service pauses the
     * current gpx recording after notifying its state. */
    private val _pauseRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val pauseRecordingSignal = _pauseRecordingSignal.asSharedFlow()

    fun pauseRecording() {
        _pauseRecordingSignal.tryEmit(Unit)
    }

    /**********************************************************************************************/

    /* The service listens to this signal. Upon reception of this signal, the service resumes the
     * current gpx recording after notifying its state. */
    private val _resumeRecordingSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resumeRecordingSignal = _resumeRecordingSignal.asSharedFlow()

    fun resumeRecording() {
        _resumeRecordingSignal.tryEmit(Unit)
    }

    /**********************************************************************************************/

    private val _gpxFileWriteEvent = MutableSharedFlow<GpxFileWriteEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val gpxFileWriteEvent = _gpxFileWriteEvent.asSharedFlow()

    fun postGpxFileWriteEvent(event: GpxFileWriteEvent) {
        _gpxFileWriteEvent.tryEmit(event)
    }

    /**********************************************************************************************/

    private val _geoStatisticsEvent = MutableSharedFlow<GeoStatistics?>(1, 0, BufferOverflow.DROP_OLDEST)
    val geoStatisticsEvent = _geoStatisticsEvent.asSharedFlow()

    fun postGeoStatistics(stats: GeoStatistics?) {
        _geoStatisticsEvent.tryEmit(stats)
    }
}

sealed interface LiveRouteEvent
data class LiveRoutePoint(val pt: TrackPoint) : LiveRouteEvent
object LiveRouteStop : LiveRouteEvent
object LiveRoutePause : LiveRouteEvent
