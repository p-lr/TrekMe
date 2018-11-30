package com.peterlaurence.trekme.core.providers.generic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Given the level, row and col numbers, this object returns a [Bitmap] using the provided [UrlTileBuilder]
 * to build an [URL] and make an HTTP request.
 */
open class GenericBitmapProvider(open val urlTileBuilder: UrlTileBuilder, options: BitmapFactory.Options? = null) {
    var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }

    open fun getBitmap(level: Int, row: Int, col: Int): Bitmap? {
        val url = URL(urlTileBuilder.build(level, row, col))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true

        return try {
            connection.connect()
            val inputStream = BufferedInputStream(connection.inputStream)
            val myBitmap = BitmapFactory.decodeStream(inputStream, null, bitmapLoadingOptions)
            inputStream.close()
            myBitmap
        } catch (e: Exception) {
            connection.disconnect()
            e.printStackTrace()
            null
        }
    }

    open fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}