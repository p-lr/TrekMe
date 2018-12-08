package com.peterlaurence.trekme.model.providers.bitmap

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekme.core.mapsource.IGNCredentials
import com.peterlaurence.trekme.core.providers.bitmap.GenericBitmapProvider
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile


/**
 * Luckily, IGN's [WMTS service](https://geoservices.ign.fr/documentation/geoservices/wmts.html) has
 * a grid coordinates that is exactly the same as the one [TileView] uses. <br>
 * Consequently, to make a valid HTTP request, we just have to format the URL with raw zoom-level,
 * row and col numbers. <br>
 * Additional information have to be provided though, like IGN credentials.
 *
 * @author peterLaurence on 11/05/18
 */
class BitmapProviderIgn(credentials: IGNCredentials, layer: String = IgnLayers.ScanExpressStandard.realName) : BitmapProvider {
    private val genericProvider: GenericBitmapProvider

    init {
        val urlTileBuilder = UrlTileBuilderIgn(credentials.api ?: "", layer)
        genericProvider = GenericBitmapProvider.getBitmapProviderIgn(urlTileBuilder, credentials.user
                ?: "", credentials.pwd ?: "")
    }


    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        val zoomLvl = tile.data as Int

        return genericProvider.getBitmap(zoomLvl, tile.row, tile.column)
    }
}