package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A specific [TileStreamProvider] for OpenStreetMap.
 *
 * @author P.Laurence on 20/16/19
 */
class TileStreamProviderOSM(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val requestProperties = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36"
    )
    private val base = TileStreamProviderRetry(TileStreamProviderHttp(urlTileBuilder, requestProperties))

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        return base.getTileStream(row, col, zoomLvl)
    }
}