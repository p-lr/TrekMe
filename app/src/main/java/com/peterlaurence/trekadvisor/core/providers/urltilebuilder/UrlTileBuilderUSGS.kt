package com.peterlaurence.trekadvisor.core.providers.urltilebuilder

class UrlTileBuilderUSGS : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/WMTS/tile/1.0.0/USGSTopo/default/GoogleMapsCompatible/$level/$row/$col"
    }
}