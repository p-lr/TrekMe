package com.peterlaurence.trekme.core.map.domain.models

import android.net.Uri
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSpec
import java.util.*

class DownloadMapRequest(
    val source: MapSourceData,
    val mapSpec: MapSpec,
    val numberOfTiles: Long,
    val tileStreamProvider: TileStreamProvider,
    val geoRecordUris: Set<Uri> = emptySet(),
    val excursionIds: Set<String> = emptySet()
)

sealed class MapDownloadEvent
data class MapDownloadPending(var progress: Int = 100): MapDownloadEvent()
data class MapDownloadFinished(val mapId: UUID): MapDownloadEvent()
object MapDownloadStorageError: MapDownloadEvent()
object MapDownloadAlreadyRunning: MapDownloadEvent()