package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec

/**
 * This event is sent by entities to have a map downloaded.
 */
class RequestDownloadMapEvent(val source: MapSource, val mapSpec: MapSpec, val numberOfTiles: Long)