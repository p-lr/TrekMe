package com.peterlaurence.trekme.core.map.mapbuilder

import com.peterlaurence.trekme.core.map.MAP_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MapGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.wmts.MapSpec
import java.io.File

fun buildMap(mapSpec: MapSpec, mapOrigin: Map.MapOrigin,
             source: WmtsSource, folder: File, imageExtension: String = ".jpg"): Map {
    val mapGson = MapGson()

    mapGson.levels = (mapSpec.levelMin..mapSpec.levelMax).map {
        MapGson.Level().apply {
            level = it - mapSpec.levelMin
            tile_size = MapGson.Level.TileSize().apply {
                x = mapSpec.tileSize
                y = mapSpec.tileSize
            }
        }
    }

    mapGson.provider = MapGson.Provider().apply {
        generated_by = mapOrigin
        wmts_source = source
        image_extension = imageExtension
    }

    mapGson.size = MapGson.MapSize().apply {
        x = mapSpec.mapWidthPx
        y = mapSpec.mapHeightPx
    }

    mapGson.name = folder.name

    mapGson.calibration.calibration_method = MapLoader.CalibrationMethod.SIMPLE_2_POINTS.name

    val jsonFile = File(folder, MAP_FILENAME)

    return Map(mapGson, jsonFile, null)
}