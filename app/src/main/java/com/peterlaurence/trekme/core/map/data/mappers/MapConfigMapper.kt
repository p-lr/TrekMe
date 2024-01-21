package com.peterlaurence.trekme.core.map.data.mappers

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.map.data.models.BoundaryKtx
import com.peterlaurence.trekme.core.map.data.models.CalibrationPointKtx
import com.peterlaurence.trekme.core.map.data.models.CreationDataKtx
import com.peterlaurence.trekme.core.map.data.models.IgnLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.IgnOverlayLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.IgnPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.IgnSpainLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.IgnSpainPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.LayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.LevelKtx
import com.peterlaurence.trekme.core.map.data.models.MapKtx
import com.peterlaurence.trekme.core.map.data.models.MapProvider
import com.peterlaurence.trekme.core.map.data.models.MapSize
import com.peterlaurence.trekme.core.map.data.models.MapSource
import com.peterlaurence.trekme.core.map.data.models.OrdnanceSurveyLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.OrdnanceSurveyPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.OsmLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.OsmPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.OverlayKtx
import com.peterlaurence.trekme.core.map.data.models.ProjectedCoordinatesKtx
import com.peterlaurence.trekme.core.map.data.models.ProjectionKtx
import com.peterlaurence.trekme.core.map.data.models.SwissLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.SwissPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.data.models.TileSize
import com.peterlaurence.trekme.core.map.data.models.UsgsLayerDataKtx
import com.peterlaurence.trekme.core.map.data.models.UsgsPrimaryLayerIdKtx
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.wmts.domain.model.Cadastre
import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.LayerPropertiesIgn
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OpenTopoMap
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmAndHd
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.PlanIgnV2
import com.peterlaurence.trekme.core.wmts.domain.model.Road
import com.peterlaurence.trekme.core.wmts.domain.model.Satellite
import com.peterlaurence.trekme.core.wmts.domain.model.Slopes
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.core.wmts.domain.model.WorldTopoMap
import java.util.UUID
import com.peterlaurence.trekme.core.map.data.models.Calibration as CalibrationKtx


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
        elevationFix = elevationFix,
        creationData = creationData?.toDomain()
    )
}

/**
 * For instance, only support EPSG:3857.
 * SRID was introduced on 2024-01.
 */
private fun ProjectionKtx.toDomain(): Projection? {
    return when (srid) {
        3857 -> MercatorProjection()
        else -> null
    }
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

private fun CreationDataKtx.toDomain(): CreationData {
    return CreationData(
        minLevel = minLevel,
        maxLevel = maxLevel,
        boundary = Boundary(
            srid = boundary.srid,
            corner1 = boundary.corner1.toDomain(),
            corner2 = boundary.corner2.toDomain()
        ),
        mapSourceData = layerData.toDomain(),
        creationDate = creationDate
    )
}

private fun ProjectedCoordinatesKtx.toDomain(): ProjectedCoordinates {
    return ProjectedCoordinates(x, y)
}

private fun LayerDataKtx.toDomain(): MapSourceData {
    return when (this) {
        is IgnLayerDataKtx -> {
            val primaryLayer = when (this.primaryLayerId) {
                IgnPrimaryLayerIdKtx.IgnClassic -> IgnClassic
                IgnPrimaryLayerIdKtx.IgnPlanV2 -> PlanIgnV2
                IgnPrimaryLayerIdKtx.IgnSatellite -> Satellite
            }
            val overlays = this.overlays.map {
                when (it.id) {
                    IgnOverlayLayerIdKtx.IgnCadastre -> LayerPropertiesIgn(Cadastre, it.opacity)
                    IgnOverlayLayerIdKtx.IgnRoad -> LayerPropertiesIgn(Road, it.opacity)
                    IgnOverlayLayerIdKtx.IgnSlopes -> LayerPropertiesIgn(Slopes, it.opacity)
                }
            }
            IgnSourceData(primaryLayer, overlays)
        }

        is OsmLayerDataKtx -> {
            OsmSourceData(
                layer = when (this.primaryLayerId) {
                    OsmPrimaryLayerIdKtx.OpenTopoMap -> OpenTopoMap
                    OsmPrimaryLayerIdKtx.OsmandHd -> OsmAndHd
                    OsmPrimaryLayerIdKtx.CustomOutdoors -> Outdoors
                    OsmPrimaryLayerIdKtx.OpenStreetMap -> WorldStreetMap
                    OsmPrimaryLayerIdKtx.ArcgisWorldTopoMap -> WorldTopoMap
                }
            )
        }

        is IgnSpainLayerDataKtx -> IgnSpainData
        is OrdnanceSurveyLayerDataKtx -> OrdnanceSurveyData
        is SwissLayerDataKtx -> SwissTopoData
        is UsgsLayerDataKtx -> UsgsData
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
        },
        creationData = creationData?.let {
            CreationDataKtx(
                minLevel = it.minLevel,
                maxLevel = it.maxLevel,
                boundary = BoundaryKtx(
                    srid = it.boundary.srid,
                    corner1 = it.boundary.corner1.toData(),
                    corner2 = it.boundary.corner2.toData()
                ),
                layerData = it.mapSourceData.toData(),
                creationDate = it.creationDate
            )
        }
    )
}

private fun MapSourceData.toData(): LayerDataKtx {
    return when (this) {
        is IgnSourceData -> {
            IgnLayerDataKtx(
                primaryLayerId = when (layer) {
                    IgnClassic -> IgnPrimaryLayerIdKtx.IgnClassic
                    PlanIgnV2 -> IgnPrimaryLayerIdKtx.IgnPlanV2
                    Satellite -> IgnPrimaryLayerIdKtx.IgnSatellite
                },
                overlays = overlays.map {
                    val id: IgnOverlayLayerIdKtx = when (it.layer) {
                        Cadastre -> IgnOverlayLayerIdKtx.IgnCadastre
                        Road -> IgnOverlayLayerIdKtx.IgnRoad
                        Slopes -> IgnOverlayLayerIdKtx.IgnSlopes
                    }
                    OverlayKtx(id = id, opacity = it.opacity)
                }
            )
        }

        is IgnSpainData -> IgnSpainLayerDataKtx(primaryLayerId = IgnSpainPrimaryLayerIdKtx.IgnSpain)
        is OrdnanceSurveyData -> OrdnanceSurveyLayerDataKtx(primaryLayerId = OrdnanceSurveyPrimaryLayerIdKtx.OrdnanceSurvey)
        is OsmSourceData -> {
            OsmLayerDataKtx(
                primaryLayerId = when (layer) {
                    OpenTopoMap -> OsmPrimaryLayerIdKtx.OpenTopoMap
                    OsmAndHd -> OsmPrimaryLayerIdKtx.OsmandHd
                    Outdoors -> OsmPrimaryLayerIdKtx.CustomOutdoors
                    WorldStreetMap -> OsmPrimaryLayerIdKtx.OpenStreetMap
                    WorldTopoMap -> OsmPrimaryLayerIdKtx.ArcgisWorldTopoMap
                }
            )
        }

        is SwissTopoData -> SwissLayerDataKtx(primaryLayerId = SwissPrimaryLayerIdKtx.SwissTopo)
        is UsgsData -> UsgsLayerDataKtx(primaryLayerId = UsgsPrimaryLayerIdKtx.UsgsTopo)
    }
}

private fun ProjectedCoordinates.toData(): ProjectedCoordinatesKtx {
    return ProjectedCoordinatesKtx(x, y)
}

private fun Projection.toData(): ProjectionKtx {
    return ProjectionKtx(srid = srid)
}

