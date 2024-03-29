package com.peterlaurence.trekme.core.wmts.data.provider

import com.peterlaurence.trekme.core.map.domain.models.OutOfBounds
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.data.model.TileStreamProviderHttp
import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder

/**
 * A specific [TileStreamProvider] for USA USGS.
 *
 * @author P.Laurence on 20/16/19
 */
class TileStreamProviderUSGS(urlTileBuilder: UrlTileBuilder) : TileStreamProvider {
    private val base = TileStreamProviderHttp(urlTileBuilder)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Safeguard */
        if (zoomLvl > 17) return OutOfBounds

        return base.getTileStream(row, col, zoomLvl)
    }
}