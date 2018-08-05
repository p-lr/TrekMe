package com.peterlaurence.trekadvisor.core.providers.urltilebuilder

class UrlTileBuilderIgn(private val api: String) : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "https://wxs.ign.fr/$api/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&LAYER=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&EXCEPTIONS=text/xml&FORMAT=image/jpeg&TILEMATRIXSET=PM&TILEMATRIX=$level&TILEROW=$row&TILECOL=$col&"
    }
}