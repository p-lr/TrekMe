package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.util.gpx.model.Gpx

class GpxRepository {
    var gpxForElevation: GpxForElevation? = null
        private set

    fun setGpxForElevation(gpx: Gpx, id: Int) {
        gpxForElevation = GpxForElevation(gpx, id)
    }

}

/**
 * Contains a [Gpx] along with a unique [id].
 */
data class GpxForElevation(val gpx: Gpx, val id: Int)