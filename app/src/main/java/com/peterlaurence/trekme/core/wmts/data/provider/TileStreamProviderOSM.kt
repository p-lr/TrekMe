package com.peterlaurence.trekme.core.wmts.data.provider

import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.data.model.TileStreamProviderHttp
import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder
import kotlin.random.Random

/**
 * A specific [TileStreamProvider] for OpenStreetMap.
 *
 * @author P.Laurence on 20/16/19
 */
class TileStreamProviderOSM(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val requestProperties = mapOf(
        "User-Agent" to generateRandomUserAgent()
    )
    private val base = TileStreamProviderHttp(urlTileBuilder, requestProperties)

    private fun generateRandomUserAgent(): String {
        val length = Random.nextInt(5, 25)
        return (0..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        return base.getTileStream(row, col, zoomLvl)
    }
}

private val charPool = arrayOf(
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
    'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6',
    '7', '8', '9'
)