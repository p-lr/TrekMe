package com.peterlaurence.trekme.features.record.app.service.event

import com.peterlaurence.trekme.core.map.domain.models.BoundingBox

data class NewExcursionEvent(val excursionId: String, val boundingBox: BoundingBox?)