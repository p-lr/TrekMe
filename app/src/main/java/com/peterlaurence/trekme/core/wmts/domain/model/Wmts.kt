package com.peterlaurence.trekme.core.wmts.domain.model

import kotlin.math.pow

/**
 * wmts level are 0 based.
 * At level 0, the map corresponds to just one tile.
 */
fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}