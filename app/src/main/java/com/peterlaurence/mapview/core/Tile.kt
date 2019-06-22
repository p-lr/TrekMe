package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import android.graphics.Paint

data class Tile(val zoom: Int, val row: Int, val col: Int, val subSample: Int) {
    lateinit var bitmap: Bitmap
    var paint: Paint? = null
}

data class TileSpec(val zoom: Int, val row: Int, val col: Int, val subSample: Int = 0)

fun Tile.sameSpecAs(spec: TileSpec): Boolean {
    return zoom == spec.zoom && row == spec.row && col == spec.col && subSample == spec.subSample
}

fun Tile.samePositionAs(tile: Tile): Boolean {
    return zoom == tile.zoom && row == tile.row && col == tile.col
}