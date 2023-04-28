package com.peterlaurence.trekme.core.wmts.data.urltilebuilder

import com.peterlaurence.trekme.core.wmts.data.model.UrlTileBuilder
import com.peterlaurence.trekme.core.wmts.domain.model.Cadastre
import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnLayer
import com.peterlaurence.trekme.core.wmts.domain.model.PlanIgnV2
import com.peterlaurence.trekme.core.wmts.domain.model.Road
import com.peterlaurence.trekme.core.wmts.domain.model.Satellite
import com.peterlaurence.trekme.core.wmts.domain.model.Slopes


class UrlTileBuilderIgn(private val api: String, private val layer: IgnLayer) : UrlTileBuilder {
    override fun build(level: Int, row: Int, col: Int): String {
        val imgFormat = when (layer) {
            PlanIgnV2, Slopes, Cadastre, Road -> "png"
            else -> "jpeg"
        }
        val style = when (layer) {
            is Cadastre -> "PCI vecteur"
            else -> "normal"
        }
        val layerName = when(layer) {
            IgnClassic -> "GEOGRAPHICALGRIDSYSTEMS.MAPS"
            PlanIgnV2 -> "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2"
            Satellite -> "ORTHOIMAGERY.ORTHOPHOTOS"
            Cadastre -> "CADASTRALPARCELS.PARCELLAIRE_EXPRESS"
            Road -> "TRANSPORTNETWORKS.ROADS"
            Slopes -> "GEOGRAPHICALGRIDSYSTEMS.SLOPES.MOUNTAIN"
        }
        return "https://wxs.ign.fr/$api/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=$style&LAYER=${layerName}&EXCEPTIONS=text/xml&Format=image/$imgFormat&tilematrixset=PM&TileMatrix=$level&TileRow=$row&TileCol=$col&"
    }
}