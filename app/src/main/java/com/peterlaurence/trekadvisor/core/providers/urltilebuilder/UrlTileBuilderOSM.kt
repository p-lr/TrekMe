package com.peterlaurence.trekadvisor.core.providers.urltilebuilder

/**
 * Use the gis.sinica.edu.tw WMTS server. It provides OpenStreetMap as well as many other layers.
 * See [the capabilities](http://gis.sinica.edu.tw/worldmap/wmts?SERVICE=WMTS&REQUEST=GetCapabilities).
 */
class UrlTileBuilderOSM : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
//        return "http://gis.sinica.edu.tw/worldmap/file-exists.php?img=OSM-png-$level-$col-$row"
//        return "http://a.tile.openstreetmap.fr/osmfr/$level/$col/$row.png"
        return "https://c.tile.openstreetmap.org/$level/$col/$row.png"
    }
}