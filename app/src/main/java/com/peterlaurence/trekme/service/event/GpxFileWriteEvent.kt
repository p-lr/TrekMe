package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.util.gpx.model.Gpx
import java.io.File

data class GpxFileWriteEvent(val gpxFile: File, val gpx: Gpx)