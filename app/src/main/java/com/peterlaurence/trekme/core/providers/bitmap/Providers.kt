package com.peterlaurence.trekme.core.providers.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.map.OutOfBounds
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStream
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
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val connection = createConnection(url)

        return try {
            connection.connect()
            TileStream(BufferedInputStream(connection.inputStream))
        } catch (e: Exception) {
            e.printStackTrace()
            TileStream(null)
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
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val tileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder, requestProperties)
        val connection = tileStreamProviderHttp.createConnection(url)

        /* Set authentication */
        connection.setAuth()

        return try {
            connection.connect()
            TileStream(BufferedInputStream(connection.inputStream))
        } catch (e: Exception) {
            connection.disconnect()
            e.printStackTrace()
            TileStream(null)
        }
    }

    private fun HttpURLConnection.setAuth() {
        setRequestProperty("User-Agent", userAgent)
    }
}

class TileStreamProviderRetry(private val tileStreamProvider: TileStreamProvider,
                              private val retryCount: Int = 30) : TileStreamProvider {
    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        var retryCnt = 0
        var result: TileResult
        do {
            result = tileStreamProvider.getTileStream(row, col, zoomLvl)
            retryCnt++
        } while (result !is OutOfBounds && ((result as? TileStream)?.tileStream == null) && retryCnt <= retryCount)
        return result
    }
}

/**
 * From a [TileStreamProvider], and eventually bitmap options, get [Bitmap] from tile coordinates.
 */
class BitmapProvider(private val tileStreamProvider: TileStreamProvider, options: BitmapFactory.Options? = null) {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? {
        return try {
            val tileResult = tileStreamProvider.getTileStream(row, col, zoomLvl)
            (tileResult as? TileStream)?.tileStream.use {
                BitmapFactory.decodeStream(it, null, bitmapLoadingOptions)
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


