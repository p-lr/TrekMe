package com.peterlaurence.trekme.core.providers.urltilebuilder

/**
 * As a note, the gis.sinica.edu.tw WMTS server provides OpenStreetMap as well as many other layers.
 * Beware that some servers may be too slow to be usable.
 * See [the capabilities](http://gis.sinica.edu.tw/worldmap/wmts?SERVICE=WMTS&REQUEST=GetCapabilities).
 */
class UrlTileBuilderOSM : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "http://b.tile.openstreetmap.fr/osmfr/$level/$col/$row.png"
    }
}