package com.peterlaurence.trekme.viewmodel.common.tileviewcompat

import com.peterlaurence.trekme.core.map.TileStream
import com.peterlaurence.trekme.core.map.TileStreamProvider
import ovh.plrapps.mapcompose.core.TileStreamProvider as MapComposeTileStreamProvider

fun TileStreamProvider.toMapComposeTileStreamProvider(): MapComposeTileStreamProvider {
    return MapComposeTileStreamProvider { row, col, zoomLvl ->
        val tileResult = this@toMapComposeTileStreamProvider.getTileStream(row, col, zoomLvl)
        (tileResult as? TileStream)?.tileStream
    }
}

const val TAG = "CompatibilityUtils.kt"