package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.util.gpx.model.Gpx
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class GpxRepository {
    private val _gpxForElevation = MutableSharedFlow<GpxForElevation?>(
            1, 0, BufferOverflow.DROP_OLDEST)
    var gpxForElevation = _gpxForElevation.asSharedFlow()

    fun setGpxForElevation(gpx: Gpx, id: Int) {
        _gpxForElevation.tryEmit(GpxForElevation(gpx, id))
    }

    fun resetGpxForElevation() {
        _gpxForElevation.tryEmit(null)
    }
}

/**
 * Contains a [Gpx] along with a unique [id].
 */
data class GpxForElevation(val gpx: Gpx, val id: Int)