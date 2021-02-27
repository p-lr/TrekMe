package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.OutOfBounds
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * Swiss topo maps server use the Referer to authorize WMTS tile fetch.
 */
class TileStreamProviderSwiss(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val requestProperties = mapOf(
            "Referer" to "https://wmts.geo.admin.ch",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36"
    )
    private val base = TileStreamProviderRetry(TileStreamProviderHttp(urlTileBuilder, requestProperties))

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Filter-out inaccessible tiles at lower levels */
        when (zoomLvl) {
            3 -> if (row != 2 || col != 4) return OutOfBounds
            4 -> if (row != 5 || col != 8) return OutOfBounds
            5 -> if (row != 11 || col != 16) return OutOfBounds
            6 -> if (row != 23 || col != 32) return OutOfBounds
            7 -> if (row != 44 || col < 65 || col > 68) return OutOfBounds
            8 -> if (row < 88 || row > 91 || col < 130 || col > 137) return OutOfBounds
            9 -> if (row < 176 || row > 185 || col < 263 || col > 272) return OutOfBounds
        }
        /* Safeguard */
        if (zoomLvl > 17) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}