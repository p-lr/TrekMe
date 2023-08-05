package com.peterlaurence.trekme.core.wmts.domain.model

import com.peterlaurence.trekme.R

sealed interface MapSourceData

data class IgnSourceData(val layer: IgnLayer, val overlays: List<LayerPropertiesIgn>) : MapSourceData
object SwissTopoData : MapSourceData
data class OsmSourceData(val layer: OsmLayer) : MapSourceData
object UsgsData : MapSourceData
object IgnSpainData : MapSourceData
object OrdnanceSurveyData : MapSourceData

fun MapSourceData.getNameResId(): Int {
    return when (this) {
        is IgnSourceData -> {
            when (this.layer) {
                Cadastre -> R.string.layer_ign_cadastre
                IgnClassic -> R.string.layer_ign_classic
                PlanIgnV2 -> R.string.layer_ign_plan_v2
                Road -> R.string.layer_ign_roads
                Satellite -> R.string.layer_ign_satellite
                Slopes -> R.string.layer_ign_slopes
            }
        }
        IgnSpainData -> R.string.ign_spain_source
        OrdnanceSurveyData -> R.string.ordnance_survey_source
        is OsmSourceData -> {
            when (this.layer) {
                OpenTopoMap -> R.string.layer_osm_opentopo
                Outdoors -> R.string.layer_osm_outdoors
                WorldStreetMap -> R.string.layer_osm_street
                OsmAndHd -> R.string.layer_osm_street_hd
                WorldTopoMap -> R.string.layer_osm_topo
            }
        }
        SwissTopoData -> R.string.swiss_topo_source
        UsgsData -> R.string.usgs_map_source
    }
}