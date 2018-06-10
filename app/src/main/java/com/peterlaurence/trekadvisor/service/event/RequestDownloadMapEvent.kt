package com.peterlaurence.trekadvisor.service.event

import com.peterlaurence.trekadvisor.core.mapsource.wmts.Tile

/**
 * This event is sent by entities to have a map downloaded.
 */
class RequestDownloadMapEvent(val url: String, val tileIterable: Iterable<Tile>)