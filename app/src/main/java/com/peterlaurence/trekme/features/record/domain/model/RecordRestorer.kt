package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox

interface RecordRestorer {
    suspend fun hasRecordToRestore(): Boolean
    suspend fun restore(): Pair<GeoRecord, BoundingBox>?
    suspend fun deleteRecord()
}