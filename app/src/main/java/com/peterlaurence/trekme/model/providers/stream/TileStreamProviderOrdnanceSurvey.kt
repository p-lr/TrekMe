package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.OutOfBounds
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A [TileStreamProvider] for U.K Ordnance Survey.
 *
 * @author Jules Cheron
 */
class TileStreamProviderOrdnanceSurvey(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base = TileStreamProviderRetry(TileStreamProviderHttp(urlTileBuilder))

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {

        when (zoomLvl) {
            7 -> if (row <= 35 || row >= 43 || col <= 60 || col >= 64) return OutOfBounds
            8 -> if (row < 71 || row > 87 || col < 120 || col > 129) return OutOfBounds
            9 -> if (row < 143 || row > 174 || col < 240 || col > 259) return OutOfBounds
            10 -> if (row < 286 || row > 349 || col < 481 || col > 518) return OutOfBounds
            11 -> if (row < 573 || row > 698 || col < 962 || col > 1036) return OutOfBounds
            12 -> if (row < 1146 || row > 1397 || col < 1925 || col > 2072) return OutOfBounds
            13 -> if (row < 2292 || row > 2794 || col < 3851 || col > 4144) return OutOfBounds
            14 -> if (row < 4584 || row > 5589 || col < 7702 || col > 8289) return OutOfBounds
            15 -> if (row < 9169 || row > 11179 || col < 15404 || col > 16579) return OutOfBounds
            16 -> if (row < 18338 || row > 22359 || col < 30808 || col > 33158) return OutOfBounds
        }

        /* Safeguard - levels above 16 are premium content */
        if (zoomLvl < 7) return OutOfBounds
        if (zoomLvl > 16) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}