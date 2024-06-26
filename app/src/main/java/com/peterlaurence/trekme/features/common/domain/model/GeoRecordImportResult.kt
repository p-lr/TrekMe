package com.peterlaurence.trekme.features.common.domain.model

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route

sealed interface GeoRecordImportResult {
    data class GeoRecordImportOk(
        val map: Map, val routes: List<Route>, val wayPoints: List<Marker>,
        val newRouteCount: Int, val newMarkersCount: Int
    ) : GeoRecordImportResult

    data object GeoRecordOutOfBounds : GeoRecordImportResult
    data object GeoRecordImportError : GeoRecordImportResult
}