package com.peterlaurence.trekme.core.map.data.mappers

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.map.data.models.MapGson
import com.peterlaurence.trekme.core.map.data.models.MapGson.MapSource
import com.peterlaurence.trekme.core.map.domain.models.*
import java.util.UUID


fun MapGson.toDomain(elevationFix: Int, thumbnailImage: Bitmap?): MapConfig? {

    calibration ?: return null
    provider ?: return null
    val calibrationMethod = runCatching {
        CalibrationMethod.valueOf(calibration.calibration_method.uppercase())
    }.getOrNull() ?: return null

    val origin = getMapOrigin(provider.generated_by)

    return MapConfig(
        uuid = getOrCreateUUID(this),
        name = name,
        thumbnail = thumbnail,
        thumbnailImage = thumbnailImage,
        levels = levels.map {
            Level(
                it.level, it.tile_size.let { tileSize ->
                    Size(tileSize.x, tileSize.y)
                }
            )
        },
        origin = origin,
        size = Size(size.x, size.y),
        imageExtension = provider.image_extension,
        calibration = Calibration(
            projection = calibration.projection,
            calibrationMethod = calibrationMethod,
            calibrationPoints = calibration.calibrationPoints.map { pt ->
                CalibrationPoint(pt.x, pt.y, pt.proj_x, pt.proj_y)
            }
        ),
        elevationFix = elevationFix
    )
}

private fun getOrCreateUUID(mapGson: MapGson): UUID {
    return mapGson.uuid?.let { uuid ->
        runCatching { UUID.fromString(uuid) }.getOrNull()
    } ?: UUID.randomUUID()
}

private fun getMapOrigin(source: MapSource): MapOrigin {
    return when (source) {
        MapSource.IGN_LICENSED -> Ign(licensed = true)
        MapSource.IGN_FREE -> Ign(licensed = false)
        MapSource.WMTS_LICENSED -> Wmts(licensed = true)
        MapSource.WMTS -> Wmts(licensed = false)
        MapSource.VIPS -> Vips
    }
}

fun MapConfig.toMapGson(): MapGson {
    val mapGson = MapGson()
    mapGson.uuid = uuid.toString()
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
        generated_by = when (val origin = this@toMapGson.origin) {
            Vips -> MapSource.VIPS
            is Ign -> if (origin.licensed) MapSource.IGN_LICENSED else MapSource.IGN_FREE
            is Wmts -> if (origin.licensed) MapSource.WMTS_LICENSED else MapSource.WMTS
        }
        image_extension = this@toMapGson.imageExtension
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

    return mapGson
}

