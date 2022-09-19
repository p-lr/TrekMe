package com.peterlaurence.trekme.core.map.mapbuilder

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec
import com.peterlaurence.trekme.core.projection.MercatorProjection
import java.io.File
import java.util.UUID

fun buildMap(
    mapSpec: MapSpec, mapOrigin: MapOrigin, folder: File, imageExtension: String = ".jpg"
): Map {

    val levels = (mapSpec.levelMin..mapSpec.levelMax).map {
        Level(it - mapSpec.levelMin, tileSize = Size(mapSpec.tileSize, mapSpec.tileSize))
    }

    val size = Size(mapSpec.mapWidthPx, mapSpec.mapHeightPx)

    val calibration = Calibration(MercatorProjection(), CalibrationMethod.SIMPLE_2_POINTS, mapSpec.calibrationPoints.toList())

    val mapConfig = MapConfig(
        uuid = UUID.randomUUID(),
        name = folder.name, thumbnail = null, levels, mapOrigin, size, imageExtension,
        calibration, sizeInBytes = null
    )

    return Map(mapConfig)
}