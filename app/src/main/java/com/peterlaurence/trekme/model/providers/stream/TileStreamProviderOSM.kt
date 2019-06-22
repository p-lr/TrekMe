package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderOSM
import java.io.InputStream

/**
 * A specific [TileStreamProvider] for OpenStreetMap.
 *
 * @author peterLaurence on 20/16/19
 */
class TileStreamProviderOSM: TileStreamProvider {
    private val base: TileStreamProviderHttp

    init {
        val urlTileBuilder = UrlTileBuilderOSM()
        base = TileStreamProviderHttp(urlTileBuilder)
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return base.getTileStream(row, col, zoomLvl)
    }
}