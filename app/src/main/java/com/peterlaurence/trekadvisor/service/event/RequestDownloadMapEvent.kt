package com.peterlaurence.trekadvisor.service.event

import com.peterlaurence.trekadvisor.core.mapsource.MapSource
import com.peterlaurence.trekadvisor.core.mapsource.wmts.Tile

/**
 * This event is sent by entities to have a map downloaded.
 */
class RequestDownloadMapEvent(val source: MapSource, val tileSequence: Sequence<Tile>,
                              val numberOfTiles: Long)