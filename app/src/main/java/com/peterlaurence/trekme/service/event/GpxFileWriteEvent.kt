package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.BoundingBox
import java.io.File

data class GpxFileWriteEvent(val gpxFile: File, val geoRecord: GeoRecord, val boundingBox: BoundingBox?)