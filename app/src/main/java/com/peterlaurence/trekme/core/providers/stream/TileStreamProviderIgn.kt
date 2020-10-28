package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.OutOfBounds
import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttpAuth
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.providers.layers.ignClassic
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A [TileStreamProvider] specific for France IGN.
 * Luckily, IGN's [WMTS service](https://geoservices.ign.fr/documentation/geoservices/wmts.html) has
 * a grid coordinates that is exactly the same as the one [MapView] uses.
 * Consequently, to make a valid HTTP request, we just have to format the URL with raw zoom-level,
 * row and col numbers.
 * Additional information have to be provided though, like IGN credentials.
 *
 * @author P.Laurence on 20/06/19
 */
class TileStreamProviderIgn(urlTileBuilder: UrlTileBuilder, val layer: Layer) : TileStreamProvider {
    private val base: TileStreamProvider

    init {
        base = TileStreamProviderRetry(TileStreamProviderHttpAuth(urlTileBuilder, "TrekMe"))
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Filter-out inaccessible tiles at lower levels */
        when (zoomLvl) {
            3 -> if (layer.publicName == ignClassic) {
                if (row >= 6 || col > 7) return OutOfBounds
            } else {
                if (row > 7 || col > 7) return OutOfBounds
            }
        }
        /* Safeguard */
        if (zoomLvl > 17) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}