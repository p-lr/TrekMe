package com.peterlaurence.trekme.repositories.recording

import com.peterlaurence.trekme.util.gpx.model.Gpx

class GpxRepository {
    var gpxForElevation: GpxForElevation? = null
        private set

    fun setGpxForElevation(gpx: Gpx, id: Int) {
        gpxForElevation = GpxForElevation(gpx, id)
    }

}

data class GpxForElevation(val gpx: Gpx, val id: Int)