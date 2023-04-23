package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder

class UrlTileBuilderIgnSpain : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "https://www.ign.es/wmts/mapa-raster?Layer=MTN&Style=normal&Tilematrixset=GoogleMapsCompatible&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image/jpeg&TileMatrix=$level&TileCol=$col&TileRow=$row"
    }
}