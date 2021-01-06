package com.peterlaurence.trekme.core.providers.stream

import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStream
import com.peterlaurence.trekme.core.map.TileStreamProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class TileStreamProviderOverlay(
        private val layerPrimary: TileStreamProvider,
        private val layerSecondary: TileStreamProvider
) : TileStreamProvider {
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 180
    }

    private val options = BitmapFactory.Options().apply { inMutable = true }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        val bitmapInputStr = layerPrimary.getTileStream(row, col, zoomLvl).let {
            if (it is TileStream) it.tileStream else null
        } ?: return TileStream(null)

        val bitmap = bitmapInputStr.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return TileStream(null)

        val bitmapOverInputStr = layerSecondary.getTileStream(row, col, zoomLvl).let {
            if (it is TileStream) it.tileStream else null
        } ?: return TileStream(null)

        val bitmapOver = bitmapOverInputStr.use {
            BitmapFactory.decodeStream(bitmapOverInputStr, null, null)
        } ?: return TileStream(null)

        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmapOver, 0f, 0f, paint)

        val bos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, bos)

        return TileStream(ByteArrayInputStream(bos.toByteArray()))
    }
}
