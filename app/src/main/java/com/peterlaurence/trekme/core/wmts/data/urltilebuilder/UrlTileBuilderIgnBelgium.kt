package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder

class UrlTileBuilderIgnBelgium : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "https://cartoweb.wmts.ngi.be/1.0.0/topo/default/3857/$level/$row/$col.png"
    }
}