package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.util.gpx.model.Gpx

data class GpxFileWriteEvent(val gpx: Gpx)