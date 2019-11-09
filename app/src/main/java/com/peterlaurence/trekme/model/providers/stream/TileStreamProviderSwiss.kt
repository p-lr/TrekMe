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
        /* Filter-out inaccessible tiles at lower levels */
        when(zoomLvl) {
            3 -> if (row != 2 || col != 4) return null
            4 -> if (row != 5 || col != 8) return null
            5 -> if (row != 11 || col != 16) return null
            6 -> if (row != 23 || col != 32) return null
            7 -> if (row != 44 || col < 65 || col > 68) return null
            8 -> if (row < 88 || row > 91 || col < 130 || col > 137) return null
            9 -> if (row < 176 || row > 185 || col < 263 || col > 272) return null
        }
        /* Safeguard */
        if (zoomLvl > 17) return null

        return base.getTileStream(row, col, zoomLvl)
    }
}