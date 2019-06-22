package com.peterlaurence.trekme.viewmodel.common.tileviewcompat

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.mapview.core.TileStreamProvider as MapViewTileStreamProvider
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderDefault
import java.io.InputStream


/**
 * This utility function converts the [Map]'s [TileStreamProvider] into whatever's type needed by
 * the view that fragments use to display tiles.
 * For instance, fragments use MapView, so the returned type is [MapViewTileStreamProvider].
 */
fun makeTileStreamProvider(map: Map): MapViewTileStreamProvider {
    return when (val x = map.tileStreamProvider) {
        is TileStreamProviderDefault -> object : MapViewTileStreamProvider {
            override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
                return x.getTileStream(row, col, zoomLvl)
            }
        }

        else -> throw NotImplementedError()
    }
}

fun TileStreamProvider.toMapViewTileStreamProvider(): MapViewTileStreamProvider {
    return object : MapViewTileStreamProvider {
        override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
            return this@toMapViewTileStreamProvider.getTileStream(row, col, zoomLvl)
        }
    }
}