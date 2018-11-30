package com.peterlaurence.trekme.service.event

import com.peterlaurence.trekme.core.map.gson.MapGson.Calibration.CalibrationPoint
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.wmts.Tile

/**
 * This event is sent by entities to have a map downloaded.
 */
class RequestDownloadMapEvent(val source: MapSource, val tileSequence: Sequence<Tile>,
                              val calibrationPoints: Pair<CalibrationPoint, CalibrationPoint>,
                              val numberOfTiles: Long)