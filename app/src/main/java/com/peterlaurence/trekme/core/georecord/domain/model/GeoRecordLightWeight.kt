package com.peterlaurence.trekme.core.georecord.domain.model

import java.util.*

sealed interface GeoRecordLightWeightState {
    data object Loading: GeoRecordLightWeightState
    data class GeoRecordLightWeightList(val geoRecords: List<GeoRecordLightWeight>): GeoRecordLightWeightState
}


data class GeoRecordLightWeight(val id: UUID, val name: String)


