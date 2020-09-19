package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec

/**
 * This event is sent by entities to have a map downloaded.
 */
class RequestDownloadMapEvent(val source: WmtsSource, val mapSpec: MapSpec, val numberOfTiles: Long, val tileStreamProvider: TileStreamProvider)