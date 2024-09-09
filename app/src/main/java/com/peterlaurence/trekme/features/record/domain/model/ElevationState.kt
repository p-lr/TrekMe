package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource

sealed interface ElevationState
object Calculating : ElevationState
data class ElevationData(
    val id: String,
    val geoRecord: GeoRecord,
    val segmentElePoints: List<SegmentElePoints> = listOf(),
    val eleMin: Double = 0.0,
    val eleMax: Double = 0.0,
    val elevationSource: ElevationSource,
    val needsUpdate: Boolean,
    val sampling: Int
) : ElevationState
object NoElevationData : ElevationState