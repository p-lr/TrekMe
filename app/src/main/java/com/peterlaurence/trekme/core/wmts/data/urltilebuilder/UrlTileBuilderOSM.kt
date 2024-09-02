package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder
import com.peterlaurence.trekme.core.wmts.domain.model.CyclOSM
import com.peterlaurence.trekme.core.wmts.domain.model.OpenTopoMap
import com.peterlaurence.trekme.core.wmts.domain.model.OsmAndHd
import com.peterlaurence.trekme.core.wmts.domain.model.OsmLayer
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.core.wmts.domain.model.WorldTopoMap

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
class UrlTileBuilderOSM(private val layer: OsmLayer) : UrlTileBuilder {
    private val serverList = listOf("a", "b", "c")

    override fun build(level: Int, row: Int, col: Int): String {
        return when (layer) {
            OpenTopoMap -> {
                val server = serverList.random()
                "https://$server.tile.opentopomap.org/$level/$col/$row.png"
            }
            CyclOSM -> {
                val server = serverList.random()
                "https://$server.tile-cyclosm.openstreetmap.fr/cyclosm/$level/$col/$row.png"
            }
            WorldStreetMap -> "https://tile.openstreetmap.org/$level/$col/$row.png"
            WorldTopoMap -> "https://services.arcgisonline.com/arcgis/rest/services/World_Topo_Map/MapServer/WMTS/tile/1.0.0/World_Topo_Map/default/GoogleMapsCompatible/$level/$row/$col.jpg"
            Outdoors -> "https://plrapps.ovh:5800/osm/outdoors/$level/$col/$row"
            OsmAndHd -> "https://tile.osmand.net/hd/$level/$col/$row.png"
        }
    }
}