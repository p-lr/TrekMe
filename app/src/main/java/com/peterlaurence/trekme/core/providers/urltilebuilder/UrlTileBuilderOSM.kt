package com.peterlaurence.trekme.core.providers.urltilebuilder

import com.peterlaurence.trekme.core.providers.layers.openTopoMap
import com.peterlaurence.trekme.core.providers.layers.osmStreet
import com.peterlaurence.trekme.core.providers.layers.osmTopo

/**
 * For OSM, using https://tile.openstreetmap.org/
 * For TopoMap, using ARCGIS web services, see https://www.arcgis.com/home/item.html?id=d5e02a0c1f2b4ec399823fdd3c2fdebd
 *
 * As a note, the gis.sinica.edu.tw WMTS server provides OpenStreetMap as well as many other layers.
 * Beware that some servers may be too slow to be usable.
 * See [the capabilities](http://gis.sinica.edu.tw/worldmap/wmts?SERVICE=WMTS&REQUEST=GetCapabilities).
 *
 * Former provider: "https://tiles.wmflabs.org/hikebike/$level/$col/$row.png"
 */
class UrlTileBuilderOSM(private val layerId: String) : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return when (layerId) {
            osmStreet -> "https://tile.openstreetmap.org/$level/$col/$row.png"
            osmTopo -> "https://services.arcgisonline.com/arcgis/rest/services/World_Topo_Map/MapServer/WMTS/tile/1.0.0/World_Topo_Map/default/GoogleMapsCompatible/$level/$row/$col.jpg"
            openTopoMap -> {
                val server = listOf("a", "b", "c").random()
                "https://$server.tile.opentopomap.org/$level/$col/$row.png"
            }
            else -> "https://tile.openstreetmap.org/$level/$col/$row.png"
        }
    }
}