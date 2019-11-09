package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.InputStream

/**
 * A specific [TileStreamProvider] for Spain IGN.
 *
 * @author peterLaurence on 20/16/19
 */
class TileStreamProviderIgnSpain(urlTileBuilder: UrlTileBuilder): TileStreamProvider {
    private val base: TileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        /* Filter-out inaccessible tiles at lower levels */
        when(zoomLvl) {
            3 -> if (row < 1 || row > 3 || col < 2 || col > 4) return null
            4 -> if (row < 3 || row > 6 || col < 5 || col > 9) return null
            5 -> if (row < 7 || row > 13 || col < 11 || col > 19) return null
            6 -> if (row < 19 || row > 27 || col < 27 || col > 35) return null
        }
        /* Safeguard */
        if (zoomLvl > 17) return null

        return base.getTileStream(row, col, zoomLvl)
    }
}