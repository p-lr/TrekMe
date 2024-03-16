package com.peterlaurence.trekme.main.eventhandler

import androidx.compose.runtime.Composable
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.app.service.event.NewExcursionEvent
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle

@Composable
fun RecordingEventHandler(
    gpxRecordEvents: GpxRecordEvents,
    onNewExcursion: (NewExcursionEvent) -> Unit
) {
    LaunchedEffectWithLifecycle(gpxRecordEvents.newExcursionEvent) { event ->
        onNewExcursion(event)
    }
}