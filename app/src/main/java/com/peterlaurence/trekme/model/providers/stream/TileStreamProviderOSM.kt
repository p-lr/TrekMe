package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileResult
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttp
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderRetry
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder

/**
 * A specific [TileStreamProvider] for OpenStreetMap.
 *
 * @author P.Laurence on 20/16/19
 */
class TileStreamProviderOSM(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base = TileStreamProviderRetry(TileStreamProviderHttp(urlTileBuilder))

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        return base.getTileStream(row, col, zoomLvl)
    }
}