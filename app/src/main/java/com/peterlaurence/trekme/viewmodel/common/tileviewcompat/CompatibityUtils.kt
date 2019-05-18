package com.peterlaurence.trekme.viewmodel.common.tileviewcompat

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderDefault
import com.qozix.tileview.graphics.BitmapProvider

/**
 * This utility function converts the [Map]'s [TileStreamProvider] into whatever's type needed by
 * the view that fragments use which to display tiles.
 * For instance, fragments use TileView, so the returned type is [BitmapProvider].
 */
fun makeBitmapProvider(map: Map): BitmapProvider {
    return when (map.tileStreamProvider) {
        is TileStreamProviderDefault -> BitmapProviderLibVips(map.tileStreamProvider)
        else -> throw NotImplementedError()
    }
}