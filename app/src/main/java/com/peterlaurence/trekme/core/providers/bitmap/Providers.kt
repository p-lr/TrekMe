package com.peterlaurence.trekme.core.providers.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
class TileStreamProviderHttp(private val urlTileBuilder: UrlTileBuilder, private val requestProperties: Map<String, String> = mapOf()) : TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val connection = createConnection(url)

        return try {
            connection.connect()
            BufferedInputStream(connection.inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createConnection(url: URL): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        requestProperties.forEach {
            connection.setRequestProperty(it.key, it.value)
        }
        return connection
    }
}

/**
 * Same as [TileStreamProviderHttp], but using user-agent authentication.
 */
class TileStreamProviderHttpAuth(private val urlTileBuilder: UrlTileBuilder, private val userAgent: String,
                                 private val requestProperties: Map<String, String> = mapOf()) : TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val tileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder, requestProperties)
        val connection = tileStreamProviderHttp.createConnection(url)

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
        setRequestProperty("User-Agent", userAgent)
    }
}

class TileStreamProviderRetry(private val tileStreamProvider: TileStreamProvider,
                              private val retryCount: Int = 10) : TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        var retryCnt = 0
        var result: InputStream? = null
        do {
            result = tileStreamProvider.getTileStream(row, col, zoomLvl)
            retryCnt++
        } while (result == null && retryCnt <= retryCount)
        return result
    }
}

/**
 * From a [TileStreamProvider], and eventually bitmap options, get [Bitmap] from tile coordinates.
 */
open class BitmapProvider(private val tileStreamProvider: TileStreamProvider, options: BitmapFactory.Options? = null) {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }

    open fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? {
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

/**
 * Adds retry behavior on a [BitmapProvider].
 */
class BitmapProviderRetry(private val retryCount: Int, private val delayMs: Long,
                          tileStreamProvider: TileStreamProvider,
                          options: BitmapFactory.Options? = null) : BitmapProvider(tileStreamProvider, options) {
    override fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? {
        var retry = 0
        var bitmap = super.getBitmap(row, col, zoomLvl)
        while (retry < retryCount && bitmap == null) {
            try {
                Thread.sleep(delayMs)
            } catch (e: Exception) {
                // don't care
            }

            bitmap = super.getBitmap(row, col, zoomLvl)

            retry++
            if (bitmap != null) {
                Log.d("BitmapProviderRetry", "Got bitmap (row col level) $row $col $zoomLvl after $retry retries")
                break
            }
        }
        if (bitmap == null) {
            Log.e("BitmapProviderRetry", "Couldn't get bitmap (row col level) $row $col $zoomLvl after $retry retries")
        }
        return bitmap
    }
}


