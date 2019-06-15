package com.peterlaurence.mapview.core

import android.graphics.Bitmap

data class Tile(val zoom: Int, val row: Int, val col: Int, val bitmap: Bitmap, val subSample: Int)

data class TileSpec(val zoom: Int, val row: Int, val col: Int, val subSample: Int = 0)

fun Tile.sameSpecAs(spec: TileSpec): Boolean {
    return zoom == spec.zoom && row == spec.row && col == spec.col && subSample == spec.subSample
}