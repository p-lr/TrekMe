package com.peterlaurence.trekme.viewmodel.common.tileviewcompat

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekme.core.providers.bitmap.GenericBitmapProvider
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderOSM
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile

class BitmapProviderOSM : BitmapProvider {
    private val urlTileBuilder = UrlTileBuilderOSM()
    private val genericProvider = GenericBitmapProvider.getBitmapProviderOSM(urlTileBuilder)

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        val zoomLvl = tile.data as Int

        return genericProvider.getBitmap(zoomLvl, tile.row, tile.column)
    }
}