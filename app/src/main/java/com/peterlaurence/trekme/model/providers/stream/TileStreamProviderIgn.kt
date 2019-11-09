package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.IGNCredentials
import com.peterlaurence.trekme.core.providers.bitmap.TileStreamProviderHttpAuth
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn
import java.io.InputStream

/**
 * A [TileStreamProvider] specific for France IGN.
 * Luckily, IGN's [WMTS service](https://geoservices.ign.fr/documentation/geoservices/wmts.html) has
 * a grid coordinates that is exactly the same as the one [MapView] uses. <br>
 * Consequently, to make a valid HTTP request, we just have to format the URL with raw zoom-level,
 * row and col numbers. <br>
 * Additional information have to be provided though, like IGN credentials.
 *
 * @author peterLaurence on 20/06/19
 */
class TileStreamProviderIgn(credentials: IGNCredentials, layer: String = IgnLayers.ScanExpressStandard.realName, urlTileBuilder: UrlTileBuilder): TileStreamProvider {
    private val base: TileStreamProvider

    init {
        base = TileStreamProviderHttpAuth(urlTileBuilder, credentials.user ?: "", credentials.pwd ?: "")
    }

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        return base.getTileStream(row, col, zoomLvl)
    }
}