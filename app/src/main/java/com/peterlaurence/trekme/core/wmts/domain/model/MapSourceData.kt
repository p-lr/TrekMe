package com.peterlaurence.trekme.core.wmts.domain.model

sealed interface MapSourceData

data class IgnSourceData(val layer: IgnLayer, val overlays: List<LayerPropertiesIgn>) : MapSourceData
object SwissTopoData : MapSourceData
data class OsmSourceData(val layer: OsmLayer) : MapSourceData
object UsgsData : MapSourceData
object IgnSpainData : MapSourceData
object OrdnanceSurveyData : MapSourceData