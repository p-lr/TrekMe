package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec
import com.peterlaurence.trekme.core.providers.layers.Layer

/**
 * This event is sent by entities which reclaim a map download.
 */
class RequestDownloadMapEvent(val source: WmtsSource, val layer: Layer?, val mapSpec: MapSpec, val numberOfTiles: Long, val tileStreamProvider: TileStreamProvider)