package com.peterlaurence.trekme.core.wmts.domain.model

import kotlin.math.pow

/* Size of level 18 (levels are 0-based) with 256px tiles */
const val mapSize = 67108864

/**
 * wmts level are 0 based.
 * At level 0, the map corresponds to just one tile.
 */
fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}