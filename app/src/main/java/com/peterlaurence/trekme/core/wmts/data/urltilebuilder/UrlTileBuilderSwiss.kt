package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder


/**
 * The documentation can be found [here](http://api3.geo.admin.ch/services/sdiservices.html#wmts)
 * See [the capabilities](https://wmts.geo.admin.ch/1.0.0/WMTSCapabilities.xml?lang=fr).
 */
class UrlTileBuilderSwiss : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        return "https://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/current/3857/$level/$col/$row.jpeg"
    }
}