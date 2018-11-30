package com.peterlaurence.trekme.core.providers

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekme.core.providers.generic.GenericBitmapProvider
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderUSGS
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile

class BitmapProviderUSGS : BitmapProvider {
    private val urlTileBuilder = UrlTileBuilderUSGS()
    private val genericProvider = GenericBitmapProvider(urlTileBuilder)

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        val zoomLvl = tile.data as Int

        return genericProvider.getBitmap(zoomLvl, tile.row, tile.column)
    }
}