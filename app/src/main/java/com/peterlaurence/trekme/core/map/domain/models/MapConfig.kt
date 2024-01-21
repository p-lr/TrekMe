package com.peterlaurence.trekme.core.map.domain.models

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import java.util.UUID


data class MapConfig(
    val uuid: UUID,
    val name: String,
    val thumbnailImage: Bitmap?,
    val levels: List<Level>,
    val origin: MapOrigin,
    val size: Size,
    val imageExtension: String,
    var calibration: Calibration?,
    val elevationFix: Int = 0,
    val creationData: CreationData? = null
)

data class Level(val level: Int, val tileSize: Size)

data class Size(val width: Int, val height: Int)

sealed interface MapOrigin
data class Ign(val licensed: Boolean): MapOrigin
data class Wmts(val licensed: Boolean): MapOrigin
object Vips : MapOrigin

data class CreationData(
    val minLevel: Int,
    val maxLevel: Int,
    val boundary: Boundary,
    val mapSourceData: MapSourceData,
    val creationDate: Long  // epoch seconds
)

data class Boundary(
    val srid: Int,
    val corner1: ProjectedCoordinates,
    val corner2: ProjectedCoordinates
)

data class ProjectedCoordinates(
    val x: Double,
    val y: Double
)

