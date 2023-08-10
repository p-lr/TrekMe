package com.peterlaurence.trekme.features.record.app.service.event

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox

data class NewExcursionEvent(val geoRecord: GeoRecord, val boundingBox: BoundingBox?)