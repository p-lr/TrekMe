package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MapKtx(
    val uuid: String? = null,
    val name: String,
    val thumbnail: String? = null,
    val levels: List<LevelKtx> = emptyList(),
    val provider: MapProvider? = null,
    val size: MapSize,
    val calibration: Calibration? = null,
    val sizeInBytes: Long? = null,
    @SerialName("creation-data")
    val creationData: CreationDataKtx? = null
)

@Serializable
data class LevelKtx(
    val level: Int,
    @SerialName("tile_size")
    val tileSize: TileSize
)

@Serializable
data class TileSize(val x: Int, val y: Int)

@Serializable
data class MapProvider(
    @SerialName("generated_by")
    val mapSource: MapSource,
    @SerialName("image_extension")
    val imageExtension: String
)

/**
 * A map can have several origins. Like it can come from a WMTS source, or produced using libvips.
 */
enum class MapSource {
    IGN_LICENSED,

    // special IGN WMTS source
    IGN_FREE,
    WMTS_LICENSED,
    WMTS,
    VIPS
    // Custom map
}

@Serializable
data class MapSize(val x: Int, val y: Int)

@Serializable
data class Calibration(
    val projection: ProjectionKtx? = null,
    @SerialName("calibration_method")
    val calibrationMethod: String = "",  // refers to CalibrationMethod domain enum names
    @SerialName("calibration_points")
    val calibrationPoints: List<CalibrationPointKtx> = emptyList()
)

@Serializable
data class ProjectionKtx(
    // expects epsg.io id. Defaults to Web Mercator. This default is important since previous versions
    // of TrekMe (3.x.x and below) used a projection name to identify the projection, and web mercator
    // was the only projection used.
    val srid: Int = 3857
)

/**
 * A CalibrationPoint defines a point on the map whose (x, y) relative coordinates
 * correspond to (projectionX, projectionY) as projected coordinates.
 * Values of [x] and [y] are in [0-1] interval.
 * Values of [projX] and [projY] can very well be bare latitude and longitude.
 *
 * For example, a point which has x=1 and y=1 is located at the bottom right corner of the map.
 * A point which has x=0 and y=0 is located at the top left corner of the map.
 */
@Serializable
data class CalibrationPointKtx(
    val x: Double,
    val y: Double,
    @SerialName("proj_x")
    val projX: Double,
    @SerialName("proj_y")
    val projY: Double
)

@Serializable
data class CreationDataKtx(
    @SerialName("min-level")
    val minLevel: Int,
    @SerialName("max-level")
    val maxLevel: Int,
    val boundary: BoundaryKtx,
    @SerialName("layer-data")
    val layerData: LayerDataKtx,
    @SerialName("creation_date")
    val creationDate: Long   // epoch seconds
)

@Serializable
data class BoundaryKtx(
    val srid: Int,
    val corner1: ProjectedCoordinatesKtx,
    val corner2: ProjectedCoordinatesKtx
)

@Serializable
data class ProjectedCoordinatesKtx(
    val x: Double,
    val y: Double
)

@Serializable
sealed interface LayerDataKtx

@SerialName("ign-layer-data")
@Serializable
data class IgnLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: IgnPrimaryLayerIdKtx,
    val overlays: List<OverlayKtx<IgnOverlayLayerIdKtx>> = emptyList()
) : LayerDataKtx

@SerialName("osm-layer-data")
@Serializable
data class OsmLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: OsmPrimaryLayerIdKtx,
) : LayerDataKtx

@SerialName("ign-spain-layer-data")
@Serializable
data class IgnSpainLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: IgnSpainPrimaryLayerIdKtx,
) : LayerDataKtx

@SerialName("ordnance-survey-layer-data")
@Serializable
data class OrdnanceSurveyLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: OrdnanceSurveyPrimaryLayerIdKtx,
) : LayerDataKtx

@SerialName("swiss-layer-data")
@Serializable
data class SwissLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: SwissPrimaryLayerIdKtx,
) : LayerDataKtx

@SerialName("usgs-layer-data")
@Serializable
data class UsgsLayerDataKtx(
    @SerialName("primary-layer-id")
    val primaryLayerId: UsgsPrimaryLayerIdKtx,
) : LayerDataKtx

@Serializable
class OverlayKtx<T>(
    val id: T,
    val opacity: Float = 1f
)

/* region Ids */
@Serializable
enum class IgnPrimaryLayerIdKtx {
    @SerialName("ign-classic")
    IgnClassic,

    @SerialName("ign-plan-v2")
    IgnPlanV2,

    @SerialName("ign-satellite")
    IgnSatellite,
}

@Serializable
enum class IgnOverlayLayerIdKtx {
    @SerialName("ign-cadastre")
    IgnCadastre,

    @SerialName("ign-road")
    IgnRoad,

    @SerialName("ign-slopes")
    IgnSlopes,
}


@Serializable
enum class OsmPrimaryLayerIdKtx {
    @SerialName("opentopomap")
    OpenTopoMap,

    @SerialName("osmand-hd")
    OsmandHd,

    @SerialName("custom-outdoors")
    CustomOutdoors,

    @SerialName("openstreetmap")
    OpenStreetMap,

    @SerialName("arcgis-world-topo-map")
    ArcgisWorldTopoMap,
}

@Serializable
enum class IgnSpainPrimaryLayerIdKtx {
    @SerialName("ign-spain")
    IgnSpain,
}

@Serializable
enum class OrdnanceSurveyPrimaryLayerIdKtx {
    @SerialName("ordnance-survey")
    OrdnanceSurvey,
}

@Serializable
enum class SwissPrimaryLayerIdKtx {
    @SerialName("swiss-topo")
    SwissTopo,
}

@Serializable
enum class UsgsPrimaryLayerIdKtx {
    @SerialName("usgs-topo")
    UsgsTopo
}
/* endregion */
