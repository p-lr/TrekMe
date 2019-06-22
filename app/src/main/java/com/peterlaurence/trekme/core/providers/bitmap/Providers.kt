package com.peterlaurence.trekme.core.providers.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Given the level, row and col numbers, a [TileStreamProviderHttp] returns an [InputStream] on a
 * tile using the provided [UrlTileBuilder] to build an [URL] and make an HTTP request.
 * The caller is responsible for closing the stream.
 */
class TileStreamProviderHttp(private val urlTileBuilder: UrlTileBuilder): TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true

        return try {
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Same as [TileStreamProviderHttp], but using basic authentication.
 */
class TileStreamProviderHttpAuth(private val urlTileBuilder: UrlTileBuilder, private val user: String,
                                 private val pwd: String): TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true

        /* Set authentication */
        connection.setAuth()

        return try {
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            connection.disconnect()
            e.printStackTrace()
            null
        }
    }

    private fun HttpURLConnection.setAuth() {
        val authString = "$user:$pwd"
        val authStringEnc = String(Base64.encode(authString.toByteArray(), Base64.NO_WRAP))
        setRequestProperty("Authorization", "Basic $authStringEnc")
    }
}

/**
 * From a [TileStreamProvider], and eventually bitmap options, get [Bitmap] from tile coordinates.
 */
class BitmapProvider(private val tileStreamProvider: TileStreamProvider, options: BitmapFactory.Options? = null) {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }

    fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? {
        return try {
            val inputStream = tileStreamProvider.getTileStream(row, col, zoomLvl)
            inputStream.use {
                BitmapFactory.decodeStream(inputStream, null, bitmapLoadingOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}


