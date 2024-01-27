package com.peterlaurence.trekme.core.map.domain.models

import android.net.Uri
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Point
import java.util.*

sealed interface MapDownloadSpec

class NewDownloadSpec(
    val source: MapSourceData,
    val corner1: Point,
    val corner2: Point,
    val minLevel: Int,
    val maxLevel: Int,
    val tileSize: Int,
    val geoRecordUris: Set<Uri> = emptySet(),
    val excursionIds: Set<String> = emptySet()
) : MapDownloadSpec

class UpdateSpec(
    val map: Map,
    val creationData: CreationData,
    val repairOnly: Boolean
) : MapDownloadSpec

sealed interface MapDownloadEvent
data class MapDownloadPending(val progress: Int = 100): MapDownloadEvent
data class MapUpdatePending(val mapId: UUID, val progress: Int = 100, val repairOnly: Boolean): MapDownloadEvent
data class MapDownloadFinished(val mapId: UUID): MapDownloadEvent
data class MapUpdateFinished(val mapId: UUID, val repairOnly: Boolean): MapDownloadEvent
data object MapDownloadStorageError: MapDownloadEvent
data object MapDownloadAlreadyRunning: MapDownloadEvent
data object MissingApiError: MapDownloadEvent
data object MapNotRepairable: MapDownloadEvent
