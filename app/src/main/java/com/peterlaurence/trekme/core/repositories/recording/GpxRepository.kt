package com.peterlaurence.trekme.core.repositories.recording

import com.peterlaurence.trekme.core.lib.gpx.model.Gpx
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO: refactor to remove the dependency on [Gpx].
 */
@Singleton
class GpxRepository @Inject constructor() {
    private val _gpxForElevation = MutableSharedFlow<GpxForElevation?>(
        1, 0, BufferOverflow.DROP_OLDEST
    )

    val gpxForElevation = _gpxForElevation.asSharedFlow()

    fun setGpxForElevation(gpx: Gpx, gpxFile: File) {
        _gpxForElevation.tryEmit(GpxForElevation(gpx, gpxFile))
    }

    fun resetGpxForElevation() {
        _gpxForElevation.tryEmit(null)
    }
}

/**
 * Contains a [Gpx] along with a unique [id].
 */
data class GpxForElevation(val gpx: Gpx, val gpxFile: File) {
    val id: UUID = UUID.randomUUID()
}