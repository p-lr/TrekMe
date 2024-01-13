package com.peterlaurence.trekme.core.map.data.mappers

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.map.data.models.CalibrationPointKtx
import com.peterlaurence.trekme.core.map.data.models.LevelKtx
import com.peterlaurence.trekme.core.map.data.models.MapKtx
import com.peterlaurence.trekme.core.map.data.models.MapProvider
import com.peterlaurence.trekme.core.map.data.models.MapSize
import com.peterlaurence.trekme.core.map.data.models.MapSource
import com.peterlaurence.trekme.core.map.data.models.ProjectionKtx
import com.peterlaurence.trekme.core.map.data.models.TileSize
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.map.data.models.Calibration as CalibrationKtx
import java.util.UUID


fun MapKtx.toDomain(elevationFix: Int, thumbnailImage: Bitmap?): MapConfig? {

    calibration ?: return null
    val imageExtension = provider?.imageExtension ?: return null
    val origin = getMapOrigin(provider.mapSource)
    val calibrationMethod = runCatching {
        CalibrationMethod.valueOf(calibration!!.calibrationMethod.uppercase())
    }.getOrNull() ?: return null

    return MapConfig(
        uuid = getOrCreateUUID(this),
        name = name,
        thumbnail = thumbnail,
        thumbnailImage = thumbnailImage,
        levels = levels.map {
            Level(
                it.level, it.tileSize.let { tileSize ->
                    Size(tileSize.x, tileSize.y)
                }
            )
        },
        origin = origin,
        size = Size(size.x, size.y),
        imageExtension = imageExtension,
        calibration = Calibration(
            projection = calibration.projection?.toDomain(),
            calibrationMethod = calibrationMethod,
            calibrationPoints = calibration.calibrationPoints.map { pt ->
                CalibrationPoint(pt.x, pt.y, pt.projX, pt.projY)
            }
        ),
        elevationFix = elevationFix
    )
}

/**
 * For instance, only support EPSG:3857
 */
private fun ProjectionKtx.toDomain(): Projection? {
    return if (name == MercatorProjection.NAME) {
        MercatorProjection()
    } else null
}

private fun getOrCreateUUID(mapKtx: MapKtx): UUID {
    return mapKtx.uuid?.let { uuid ->
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

fun MapConfig.toMapKtx(): MapKtx {
    return MapKtx(
        uuid = uuid.toString(),
        name = name,
        thumbnail = thumbnail,
        levels = levels.map { lvl ->
            LevelKtx(
                level = lvl.level,
                tileSize = lvl.tileSize.let { size ->
                    TileSize(
                        x = size.width,
                        y = size.height
                    )
                }
            )
        },
        provider = MapProvider(
            mapSource = when (val origin = this@toMapKtx.origin) {
                Vips -> MapSource.VIPS
                is Ign -> if (origin.licensed) MapSource.IGN_LICENSED else MapSource.IGN_FREE
                is Wmts -> if (origin.licensed) MapSource.WMTS_LICENSED else MapSource.WMTS
            },
            imageExtension = this@toMapKtx.imageExtension
        ),
        size = MapSize(
            x = size.width,
            y = size.height
        ),
        calibration = calibration?.let { cal ->
            CalibrationKtx(
                projection = cal.projection?.toData(),
                calibrationMethod = cal.calibrationMethod.toString(),
                calibrationPoints = cal.calibrationPoints.map { pt ->
                    CalibrationPointKtx(
                        x = pt.normalizedX,
                        y = pt.normalizedY,
                        projX = pt.absoluteX,
                        projY = pt.absoluteY,
                    )
                }
            )
        }
    )
}

/**
 * For instance, only support 3857 SRID.
 */
private fun Projection.toData(): ProjectionKtx {
    val srid = if (this is MercatorProjection) 3857 else 0
    return ProjectionKtx(name = name, srid = srid)
}

