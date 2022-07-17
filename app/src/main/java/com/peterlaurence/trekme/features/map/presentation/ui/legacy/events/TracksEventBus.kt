package com.peterlaurence.trekme.features.map.presentation.ui.legacy.events

import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.events.TrackColorChangeEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TracksEventBus {
    /* region Track name*/
    private val _trackNameChangeSignal = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackNameChangeSignal = _trackNameChangeSignal.asSharedFlow()

    fun postTrackNameChange() = _trackNameChangeSignal.tryEmit(Unit)
    /* endregion */

    /* region Track color */
    private val _trackColorChangeEvent = MutableSharedFlow<TrackColorChangeEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackColorChangeEvent = _trackColorChangeEvent.asSharedFlow()

    fun postTrackColorChange(event: TrackColorChangeEvent) = _trackColorChangeEvent.tryEmit(event)
    /* endregion */

    /* region Track import */
    private val _trackImportEvent = MutableSharedFlow<GeoRecordImportResult>(0, 1, BufferOverflow.DROP_OLDEST)
    val trackImportEvent = _trackImportEvent.asSharedFlow()

    fun postTrackImportEvent(event: GeoRecordImportResult) = _trackImportEvent.tryEmit(event)
    /* endregion */
}