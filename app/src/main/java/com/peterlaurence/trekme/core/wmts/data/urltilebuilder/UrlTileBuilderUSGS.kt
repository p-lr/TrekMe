package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsImageryTopo
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsLayer
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsTopo

class UrlTileBuilderUSGS(private val layer: UsgsLayer) : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        val layerName = when (layer) {
            UsgsImageryTopo -> "USGSImageryTopo"
            UsgsTopo -> "USGSTopo"
        }
        return "https://basemap.nationalmap.gov/arcgis/rest/services/$layerName/MapServer/WMTS/tile/1.0.0/$layerName/default/GoogleMapsCompatible/$level/$row/$col"
    }
}