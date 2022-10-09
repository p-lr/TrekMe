package com.peterlaurence.trekme.core.providers.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.map.domain.models.OutOfBounds
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Given the level, row and col numbers, a [TileStreamProviderHttp] returns an [InputStream] on a
 * tile using the provided [UrlTileBuilder] to build an [URL] and make an HTTP request.
 * The caller is responsible for closing the stream.
 */
class TileStreamProviderHttp(
    private val urlTileBuilder: UrlTileBuilder,
    private val requestProperties: Map<String, String> = mapOf()
) :
    TileStreamProvider {
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
class TileStreamProviderHttpAuth(
    private val urlTileBuilder: UrlTileBuilder, private val userAgent: String,
    requestProperties: Map<String, String> = mapOf()
) :
    TileStreamProvider {
    private val tileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder, requestProperties)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val connection = tileStreamProviderHttp.createConnection(url)

        /* Set authentication */
        connection.setAuth()

        return try {
            connection.connect()
            TileStream(BufferedInputStream(connection.inputStream))
        } catch (e: Exception) {
            connection.disconnect()
            TileStream(null)
        }
    }

    private fun HttpURLConnection.setAuth() {
        setRequestProperty("User-Agent", userAgent)
    }
}

/**
 * From a [TileStreamProvider], and eventually bitmap options, get [Bitmap] from tile coordinates.
 * Attempts up to [maxRetry] to fetch a tile.
 */
class BitmapProvider(
    private val tileStreamProvider: TileStreamProvider,
    options: BitmapFactory.Options? = null
) {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()
    private val maxRetry = 5

    suspend fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? = withContext(Dispatchers.IO) {
        var retry = 0
        var bitmap: Bitmap?
        do {
            bitmap = withTimeoutOrNull(1000) {
                runCatching {
                    when (val tileResult = tileStreamProvider.getTileStream(row, col, zoomLvl)) {
                        OutOfBounds -> {
                            retry = maxRetry // leave the loop
                            return@withTimeoutOrNull null
                        }
                        is TileStream -> {
                            BitmapFactory.decodeStream(tileResult.tileStream, null, bitmapLoadingOptions)
                        }
                    }
                }.getOrNull()
            }
            if (bitmap == null) retry++
        } while (retry < maxRetry && bitmap == null)
        bitmap
    }

    fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}


