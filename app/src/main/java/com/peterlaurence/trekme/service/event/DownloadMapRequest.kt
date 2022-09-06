package com.peterlaurence.trekme.service.event

import android.net.Uri
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec

/**
 * This event is sent by entities which reclaim a map download.
 */
class DownloadMapRequest(
    val source: WmtsSource,
    val mapSpec: MapSpec,
    val numberOfTiles: Long,
    val tileStreamProvider: TileStreamProvider,
    val geoRecordUris: Set<Uri> = emptySet()
)