package com.peterlaurence.trekme.ui.mapview.events

import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.events.TrackColorChangeEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapViewEventBus {
    /* region Track name*/
    private val _trackNameChangeSignal = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackNameChangeSignal = _trackNameChangeSignal.asSharedFlow()

    fun postTrackNameChange() = _trackNameChangeSignal.tryEmit(Unit)
    /* endregion */

    /* region Track visibility */
    private val _trackVisibilityChangedSignal = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackVisibilityChangedSignal = _trackVisibilityChangedSignal.asSharedFlow()

    fun postTrackVisibilityChange() = _trackVisibilityChangedSignal.tryEmit(Unit)
    /* endregion */

    /* region Track color */
    private val _trackColorChangeEvent = MutableSharedFlow<TrackColorChangeEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackColorChangeEvent = _trackColorChangeEvent.asSharedFlow()

    fun postTrackColorChange(event: TrackColorChangeEvent) = _trackColorChangeEvent.tryEmit(event)
    /* endregion */

    /* region Track import */
    private val _trackImportEvent = MutableSharedFlow<TrackImporter.GpxImportResult>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackImportEvent = _trackImportEvent.asSharedFlow()

    fun postTrackImportEvent(event: TrackImporter.GpxImportResult) = _trackImportEvent.tryEmit(event)
    /* endregion */

    /* region Scale ratio */
    var scaleRatio: Int? = null
        private set

    /* For now, just remember the scale ratio. Ultimately, we could store the MapState here, maybe
     * inside a SharedFlow. */
    fun rememberMapState(scaleRatio: Int) {
        this.scaleRatio = scaleRatio
    }
    /* endregion */
}