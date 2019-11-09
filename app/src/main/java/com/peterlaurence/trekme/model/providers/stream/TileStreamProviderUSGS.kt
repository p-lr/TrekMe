package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.InputStream

/**
 * A specific [TileStreamProvider] for USA USGS.
 *
 * @author peterLaurence on 20/16/19
 */
class TileStreamProviderUSGS(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base: TileStreamProviderHttp = TileStreamProviderHttp(urlTileBuilder)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        /* Safeguard */
        if (zoomLvl > 17) return null

        return base.getTileStream(row, col, zoomLvl)
    }
}