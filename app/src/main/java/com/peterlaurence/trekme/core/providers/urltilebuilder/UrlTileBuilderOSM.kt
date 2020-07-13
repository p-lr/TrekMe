package com.peterlaurence.trekme.core.providers.urltilebuilder

/**
 * As a note, the gis.sinica.edu.tw WMTS server provides OpenStreetMap as well as many other layers.
 * Beware that some servers may be too slow to be usable.
 * See [the capabilities](http://gis.sinica.edu.tw/worldmap/wmts?SERVICE=WMTS&REQUEST=GetCapabilities).
 *
 * Former provider: "https://tiles.wmflabs.org/hikebike/$level/$col/$row.png"
 */
class UrlTileBuilderOSM : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        // Currently using ARCGIS World Topo Map
        // For more info: https://www.arcgis.com/home/item.html?id=d5e02a0c1f2b4ec399823fdd3c2fdebd
        return "https://services.arcgisonline.com/arcgis/rest/services/World_Topo_Map/MapServer/WMTS/tile/1.0.0/World_Topo_Map/default/GoogleMapsCompatible/$level/$row/$col.jpg"
    }
}