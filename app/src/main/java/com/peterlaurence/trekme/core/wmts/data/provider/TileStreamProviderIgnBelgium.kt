package com.peterlaurence.trekme.core.wmts.data.provider

import com.peterlaurence.trekme.core.map.domain.models.OutOfBounds
import com.peterlaurence.trekme.core.map.domain.models.TileResult
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.data.model.TileStreamProviderHttp
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderIgnBelgium

class TileStreamProviderIgnBelgium(
    urlTileBuilder: UrlTileBuilderIgnBelgium
) : TileStreamProvider {
    private val base = TileStreamProviderHttp(urlTileBuilder)

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
        /* Safeguard */
        if (zoomLvl > 17 || zoomLvl < 7) return OutOfBounds

        when (zoomLvl) {
            7 -> if (row < 42 || row > 44 || col < 64 || col > 67) return OutOfBounds
            8 -> if (row < 85 || row > 88 || col < 129 || col > 133) return OutOfBounds
            9 -> if (row < 170 || row > 175 || col < 259 || col > 266) return OutOfBounds
            10 -> if (row < 340 || row > 350 || col < 519 || col > 531) return OutOfBounds
            11 -> if (row < 681 || row > 700 || col < 1038 || col > 1062) return OutOfBounds
            12 -> if (row < 1362 || row > 1399 || col < 2076 || col > 2123) return OutOfBounds
            13 -> if (row < 2724 || row > 2798 || col < 4153 || col > 4246) return OutOfBounds
            14 -> if (row < 5448 || row > 5595 || col < 8306 || col > 8492) return OutOfBounds
            15 -> if (row < 10896 || row > 11190 || col < 16612 || col > 16983) return OutOfBounds
            16 -> if (row < 21792 || row > 22380 || col < 33225 || col > 33965) return OutOfBounds
            17 -> if (row < 43585 || row > 44760 || col < 66450 || col > 67929) return OutOfBounds
        }

        return base.getTileStream(row, col, zoomLvl)
    }
}