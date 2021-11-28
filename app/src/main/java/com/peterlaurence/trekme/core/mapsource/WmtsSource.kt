package com.peterlaurence.trekme.core.mapsource

import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.core.repositories.mapcreate.LayerProperties


enum class WmtsSource {
    IGN, SWISS_TOPO, OPEN_STREET_MAP, USGS, IGN_SPAIN, ORDNANCE_SURVEY
}

sealed interface MapSourceData
data class IgnSourceData(val api: String, val layer: Layer, val overlays: List<LayerProperties>) :
    MapSourceData

object SwissTopoData : MapSourceData
data class OsmSourceData(val layer: Layer) : MapSourceData
object UsgsData : MapSourceData
object IgnSpainData : MapSourceData
data class OrdnanceSurveyData(val api: String) : MapSourceData
