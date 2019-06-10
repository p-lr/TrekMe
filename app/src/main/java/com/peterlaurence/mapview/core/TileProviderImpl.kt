package com.peterlaurence.mapview.core

import android.graphics.Bitmap

/**
 * An implementation of [TileProvider] that uses [BitmapPool].
 */
class TileProviderImpl(private val bitmapPool: BitmapPool, private val tileSize: Int): TileProvider {

    /**
     * Pick a [Bitmap] from the [bitmapPool] if possible. Otherwise, allocate a new [Bitmap].
     */
    override fun getTile(zoom: Int, row: Int, col: Int): Tile {
        val bitmap = bitmapPool.getBitmap() ?: Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.RGB_565)
        return Tile(zoom, row, col, bitmap)
    }

    override fun recycleTile(tile: Tile) {
        bitmapPool.putBitmap(tile.bitmap)
    }
}