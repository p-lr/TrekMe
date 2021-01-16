package com.peterlaurence.trekme.core.providers.urltilebuilder

import com.peterlaurence.trekme.core.providers.layers.Cadastre
import com.peterlaurence.trekme.core.providers.layers.IgnLayer
import com.peterlaurence.trekme.core.providers.layers.IgnLayerOverlay
import com.peterlaurence.trekme.core.providers.layers.Layer


class UrlTileBuilderIgn(private val api: String, private val layer: Layer) : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        val imgFormat = when (layer) {
            is IgnLayer -> "jpeg"
            is IgnLayerOverlay -> "png"
            else -> "jpeg"
        }
        val style = when (layer) {
            is Cadastre -> "PCI vecteur"
            else -> "normal"
        }
        return "https://wxs.ign.fr/$api/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=$style&LAYER=${layer.wmtsName}&EXCEPTIONS=text/xml&Format=image/$imgFormat&tilematrixset=PM&TileMatrix=$level&TileRow=$row&TileCol=$col&"
    }
}