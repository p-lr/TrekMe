package com.peterlaurence.trekme.core.map.mapbuilder

import com.peterlaurence.trekme.core.map.MAP_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec
import java.io.File

fun buildMap(
    mapSpec: MapSpec, mapOrigin: MapOrigin, folder: File, imageExtension: String = ".jpg"
): Map {

    val levels = (mapSpec.levelMin..mapSpec.levelMax).map {
        Level(it - mapSpec.levelMin, tileSize = Size(mapSpec.tileSize, mapSpec.tileSize))
    }

    val size = Size(mapSpec.mapWidthPx, mapSpec.mapHeightPx)

    val calibration = Calibration(null, CalibrationMethod.SIMPLE_2_POINTS, listOf())

    val mapConfig = MapConfig(
        name = folder.name, thumbnail = null, levels, mapOrigin, size, imageExtension,
        calibration, sizeInBytes = null
    )
    val jsonFile = File(folder, MAP_FILENAME)

    return Map(mapConfig, jsonFile, null)
}