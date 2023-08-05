package com.peterlaurence.trekme.core.wmts.domain.model

import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint

data class Tile(
    val level: Int, val row: Int, val col: Int, val indexLevel: Int, val indexRow: Int,
    val indexCol: Int
)

data class Point(val X: Double, val Y: Double)
data class MapSpec(
    val levelMin: Int,
    val levelMax: Int,
    val mapWidthPx: Int,
    val mapHeightPx: Int,
    val tileSequence: Sequence<Tile>,
    val calibrationPoints: Pair<CalibrationPoint, CalibrationPoint>,
    val tileSize: Int
)

const val X0 = -20037508.3427892476
const val Y0 = -X0
const val X1 = -X0
const val Y1 = -Y0

const val TILE_SIZE_IN_MO = 0.0169




