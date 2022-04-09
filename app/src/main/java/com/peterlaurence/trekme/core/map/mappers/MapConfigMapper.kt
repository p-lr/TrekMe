package com.peterlaurence.trekme.core.map.mappers

import com.peterlaurence.trekme.core.map.domain.*
import com.peterlaurence.trekme.core.map.data.MapGson


fun MapGson.toDomain(): MapConfig? {
    val origin = providerToMapOrigin[provider.generated_by] ?: return null
    val imageExtension = provider?.image_extension ?: return null
    val calibrationMethod = runCatching {
        CalibrationMethod.valueOf(calibration!!.calibration_method.uppercase())
    }.getOrNull() ?: return null

    return MapConfig(
        name = name,
        thumbnail = thumbnail,
        levels = levels.map {
            Level(
                it.level, it.tile_size.let { tileSize ->
                    Size(tileSize.x, tileSize.y)
                }
            )
        },
        origin = origin,
        size = Size(size.x, size.y),
        imageExtension = imageExtension,
        calibration = calibration?.let {
            Calibration(
                projection = it.projection,
                calibrationMethod = calibrationMethod,
                calibrationPoints = it.calibrationPoints.map { pt ->
                    CalibrationPoint(pt.x, pt.y, pt.proj_x, pt.proj_y)
                }
            )
        },
        sizeInBytes = sizeInBytes
    )
}


private val providerToMapOrigin = mapOf(
    MapGson.MapSource.IGN_LICENSED to Wmts(licensed = true),
    MapGson.MapSource.WMTS to Wmts(licensed = false),
    MapGson.MapSource.VIPS to Vips
)

fun MapConfig.toEntity(): MapGson {
    val mapGson = MapGson()
    mapGson.name = name
    mapGson.thumbnail = thumbnail
    mapGson.levels = levels.map { lvl ->
        MapGson.Level().apply {
            level = lvl.level
            tile_size = lvl.tileSize.let { size ->
                MapGson.Level.TileSize().apply {
                    x = size.width
                    y = size.height
                }
            }
        }
    }
    mapGson.provider = MapGson.Provider().apply {
        generated_by = when(val origin = this@toEntity.origin) {
            Vips -> MapGson.MapSource.VIPS
            is Wmts -> if (origin.licensed) MapGson.MapSource.IGN_LICENSED else MapGson.MapSource.WMTS
        }
        image_extension = this@toEntity.imageExtension
    }
    mapGson.size = size.let { size ->
        MapGson.MapSize().apply {
            x = size.width
            y = size.height
        }
    }
    mapGson.calibration = calibration?.let { cal ->
        MapGson.Calibration().apply {
            projection = cal.projection
            calibration_method = cal.calibrationMethod.toString()
            calibrationPoints = cal.calibrationPoints.map { pt ->
                MapGson.Calibration.CalibrationPoint().apply {
                    x = pt.normalizedX
                    y = pt.normalizedY
                    proj_x = pt.absoluteX
                    proj_y = pt.absoluteY
                }
            }
        }
    }
    mapGson.sizeInBytes = sizeInBytes

    return mapGson
}

