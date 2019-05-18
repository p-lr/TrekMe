package com.peterlaurence.trekme.core.map

import java.io.InputStream


interface TileStreamProvider {
    fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}