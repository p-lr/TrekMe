package com.peterlaurence.trekme.core.map.domain.models

import android.graphics.Bitmap
import java.util.UUID


data class MapConfig(
    val uuid: UUID,
    val name: String,
    val thumbnail: String?,
    val thumbnailImage: Bitmap?,
    val levels: List<Level>,
    val origin: MapOrigin,
    val size: Size,
    val imageExtension: String,
    var calibration: Calibration?,
    val elevationFix: Int = 0
)

data class Level(val level: Int, val tileSize: Size)

data class Size(val width: Int, val height: Int)

sealed interface MapOrigin
data class Wmts(val licensed: Boolean): MapOrigin
object Vips : MapOrigin

