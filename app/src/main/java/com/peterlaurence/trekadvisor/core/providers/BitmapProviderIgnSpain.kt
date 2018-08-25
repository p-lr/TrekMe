package com.peterlaurence.trekadvisor.core.providers

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekadvisor.core.providers.generic.GenericBitmapProvider
import com.peterlaurence.trekadvisor.core.providers.urltilebuilder.UrlTileBuilderIgnSpain
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile

class BitmapProviderIgnSpain : BitmapProvider {
    private val urlTileBuilder = UrlTileBuilderIgnSpain()
    private val genericProvider = GenericBitmapProvider(urlTileBuilder)

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        val zoomLvl = tile.data as Int

        return genericProvider.getBitmap(zoomLvl, tile.row, tile.column)
    }
}