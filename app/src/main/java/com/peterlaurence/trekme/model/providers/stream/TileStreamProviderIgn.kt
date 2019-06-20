package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.IGNCredentials
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttpAuth
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn
import java.io.InputStream

/**
 * A [TileStreamProvider] specific for France IGN
 *
 * @author peterLaurence on 20/06/19
 */
class TileStreamProviderIgn(credentials: IGNCredentials, layer: String = IgnLayers.ScanExpressStandard.realName): TileStreamProvider {
    private val base: TileStreamProvider

    init {
        val urlTileBuilder = UrlTileBuilderIgn(credentials.api ?: "", layer)
        base = TileStreamProviderHttpAuth(urlTileBuilder, credentials.user ?: "", credentials.pwd ?: "")
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return base.getTileStream(row, col, zoomLvl)
    }
}