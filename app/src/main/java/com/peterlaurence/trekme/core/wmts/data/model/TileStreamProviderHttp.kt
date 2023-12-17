package com.peterlaurence.trekme.core.wmts.data.model

import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
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

        return try {
            val connection = createConnection(url)
            connection.connect()
            TileStream(BufferedInputStream(connection.inputStream))
        } catch (e: Exception) {
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
) : TileStreamProvider {
    private val tileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder, requestProperties)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        val url = URL(urlTileBuilder.build(zoomLvl, row, col))
        val tileStream = runCatching {
            val connection = tileStreamProviderHttp.createConnection(url)

            /* Set authentication */
            connection.setAuth()

            connection.connect()
            BufferedInputStream(connection.inputStream)
        }.getOrNull()

        return TileStream(tileStream)
    }

    private fun HttpURLConnection.setAuth() {
        setRequestProperty("User-Agent", userAgent)
    }
}