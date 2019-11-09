package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.InputStream

/**
 * Swiss topo maps server use the Referer to authorize WMTS tile fetch.
 */
class TileStreamProviderSwiss(urlTileBuilder: UrlTileBuilder): TileStreamProvider {
    private val requestProperties = mapOf(
            "Referer" to "https://wmts.geo.admin.ch",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36"
    )
    private val base: TileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder, requestProperties)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return base.getTileStream(row, col, zoomLvl)
    }
}