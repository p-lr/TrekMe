package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgnSpain
import java.io.InputStream

/**
 * A specific [TileStreamProvider] for Spain IGN.
 *
 * @author peterLaurence on 20/16/19
 */
class TileStreamProviderIgnSpain: TileStreamProvider {
    private val base: TileStreamProviderHttp

    init {
        val urlTileBuilder = UrlTileBuilderIgnSpain()
        base = TileStreamProviderHttp(urlTileBuilder)
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return base.getTileStream(row, col, zoomLvl)
    }
}