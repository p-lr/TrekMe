package com.peterlaurence.trekme.core.wmts.domain.model

import com.peterlaurence.trekme.R

sealed interface MapSourceData

data class IgnSourceData(val layer: IgnPrimaryLayer, val overlays: List<LayerPropertiesIgn>) : MapSourceData
data object SwissTopoData : MapSourceData
data class OsmSourceData(val layer: OsmLayer) : MapSourceData
data object UsgsData : MapSourceData
data object IgnSpainData : MapSourceData
data object OrdnanceSurveyData : MapSourceData

fun MapSourceData.getNameResId(): Int {
    return when (this) {
        is IgnSourceData -> {
            when (this.layer) {
                IgnClassic -> R.string.layer_ign_classic
                PlanIgnV2 -> R.string.layer_ign_plan_v2
                Satellite -> R.string.layer_ign_satellite
            }
        }
        is IgnSpainData -> R.string.ign_spain_source
        is OrdnanceSurveyData -> R.string.ordnance_survey_source
        is OsmSourceData -> {
            when (this.layer) {
                OpenTopoMap -> R.string.layer_osm_opentopo
                CyclOSM -> R.string.layer_osm_cyclosm
                Outdoors -> R.string.layer_osm_outdoors
                WorldStreetMap -> R.string.layer_osm_street
                OsmAndHd -> R.string.layer_osm_street_hd
                WorldTopoMap -> R.string.layer_osm_topo
            }
        }
        is SwissTopoData -> R.string.swiss_topo_source
        is UsgsData -> R.string.usgs_map_source
    }
}