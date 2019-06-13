package com.peterlaurence.mapview.core

import android.graphics.Bitmap

data class Tile(val zoom: Int, val row: Int, val col: Int, val bitmap: Bitmap)

data class TileLocation(val zoom: Int, val row: Int, val col: Int)

fun Tile.sameLocationAs(loc: TileLocation): Boolean {
    return zoom == loc.zoom && row == loc.row && col == loc.col
}