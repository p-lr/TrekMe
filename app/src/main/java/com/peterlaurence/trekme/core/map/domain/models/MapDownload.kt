package com.peterlaurence.trekme.core.map.domain.models

import android.net.Uri
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSpec
import java.util.*

sealed interface MapDownloadSpec

class NewDownloadSpec(
    val source: MapSourceData,
    val mapSpec: MapSpec,
    val numberOfTiles: Long,
    val geoRecordUris: Set<Uri> = emptySet(),
    val excursionIds: Set<String> = emptySet()
) : MapDownloadSpec

class UpdateSpec(
    val map: Map,
    val creationData: CreationData,
    val repairOnly: Boolean
) : MapDownloadSpec

sealed interface MapDownloadEvent
sealed interface MapProgressEvent: MapDownloadEvent {
    var progress: Int
}
data class MapDownloadPending(override var progress: Int = 100): MapProgressEvent
data class MapRepairPending(override var progress: Int = 100): MapProgressEvent
data class MapUpdatePending(override var progress: Int = 100): MapProgressEvent
data class MapDownloadFinished(val mapId: UUID): MapDownloadEvent
data object MapDownloadStorageError: MapDownloadEvent
data object MapDownloadAlreadyRunning: MapDownloadEvent
data object MissingApiError: MapDownloadEvent
data object MapNotRepairable: MapDownloadEvent
