package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.domain.models.OutOfBounds
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A specific [TileStreamProvider] for Spain IGN.
 *
 * @author P.Laurence on 20/16/19
 */
class TileStreamProviderIgnSpain(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base = TileStreamProviderHttp(urlTileBuilder)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Filter-out inaccessible tiles at lower levels */
        when (zoomLvl) {
            3 -> if (row < 1 || row > 3 || col < 2 || col > 4) return OutOfBounds
            4 -> if (row < 3 || row > 6 || col < 5 || col > 9) return OutOfBounds
            5 -> if (row < 7 || row > 13 || col < 11 || col > 19) return OutOfBounds
            6 -> if (row < 19 || row > 27 || col < 27 || col > 35) return OutOfBounds
        }
        /* Safeguard */
        if (zoomLvl > 17) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}