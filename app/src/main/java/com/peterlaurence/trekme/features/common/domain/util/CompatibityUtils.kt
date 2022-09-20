package com.peterlaurence.trekme.features.common.domain.util

import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

fun TileStreamProvider.toMapComposeTileStreamProvider(): MapComposeTileStreamProvider {
    return MapComposeTileStreamProvider { row, col, zoomLvl ->
        val tileResult = this@toMapComposeTileStreamProvider.getTileStream(row, col, zoomLvl)
        (tileResult as? TileStream)?.tileStream
    }
}

const val TAG = "CompatibilityUtils.kt"