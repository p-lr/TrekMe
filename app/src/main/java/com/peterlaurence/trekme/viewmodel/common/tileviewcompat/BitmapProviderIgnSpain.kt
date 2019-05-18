package com.peterlaurence.trekme.viewmodel.common.tileviewcompat

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekme.core.providers.bitmap.GenericBitmapProvider
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgnSpain
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile

class BitmapProviderIgnSpain : BitmapProvider {
    private val urlTileBuilder = UrlTileBuilderIgnSpain()
    private val genericProvider = GenericBitmapProvider.getBitmapProviderIgnSpain(urlTileBuilder)

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        val zoomLvl = tile.data as Int

        return genericProvider.getBitmap(zoomLvl, tile.row, tile.column)
    }
}