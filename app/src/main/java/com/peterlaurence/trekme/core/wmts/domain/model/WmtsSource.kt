package com.peterlaurence.trekme.core.wmts.domain.model


enum class WmtsSource {
    IGN, SWISS_TOPO, OPEN_STREET_MAP, USGS, IGN_SPAIN, ORDNANCE_SURVEY
}

sealed interface MapSourceData

data class IgnSourceData(val layer: Layer, val overlays: List<LayerProperties>) : MapSourceData
object SwissTopoData : MapSourceData
data class OsmSourceData(val layer: Layer) : MapSourceData
object UsgsData : MapSourceData
object IgnSpainData : MapSourceData
object OrdnanceSurveyData : MapSourceData
