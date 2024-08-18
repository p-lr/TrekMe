package com.peterlaurence.trekme.core.wmts.domain.model

sealed class Layer(open val id: String)

sealed interface IgnLayer
sealed class IgnPrimaryLayer(override val id: String) : Layer(id), IgnLayer
data object IgnClassic : IgnPrimaryLayer(ignClassic)
data object PlanIgnV2 : IgnPrimaryLayer(ignPlanv2)
data object Satellite : IgnPrimaryLayer(ignSatellite)

sealed class IgnOverlayLayer(override val id: String) : Layer(id), IgnLayer
data object Road : IgnOverlayLayer(ignRoad)
data object Slopes : IgnOverlayLayer(ignSlopes)
data object Cadastre : IgnOverlayLayer(ignCadastre)

const val ignPlanv2 = "Plan IGN V2"
const val ignClassic = "Cartes IGN"
const val ignSatellite = "Photographies aériennes"
const val ignRoad = "Routes IGN"
const val ignSlopes = "Carte des pentes"
const val ignCadastre = "Parcelles cadastrales"

/* Primary IGN layers are layers which are rendered below overlay layers */
val ignLayersPrimary: List<IgnPrimaryLayer> = listOf(IgnClassic, PlanIgnV2, Satellite)

/* Overlay layers can be drawn above primary layers (e.g routes, slopes, ..) */
val ignLayersOverlay: List<IgnOverlayLayer> = listOf(Slopes, Cadastre, Road)

sealed interface OsmLayer
sealed class OsmPrimaryLayer(override val id: String) : Layer(id), OsmLayer
data object WorldTopoMap : OsmPrimaryLayer(osmTopo)
data object WorldStreetMap : OsmPrimaryLayer(osmStreet)
data object OpenTopoMap : OsmPrimaryLayer(openTopoMap)
data object Outdoors : OsmPrimaryLayer(osmOutdoors)
data object OsmAndHd : OsmPrimaryLayer(osmAndHd)

const val osmTopo = "osmTopo"
const val osmStreet = "osmStreet"
const val openTopoMap = "openTopoMap"
const val osmOutdoors = "osmOutdoors"
const val osmAndHd = "osmAndHd"

/* All supported OSM layers
 * As of 2024/08/18, remove OpenTopoMap as levels 16 and 17 are no longer available */
val osmLayersPrimary: List<OsmPrimaryLayer> = listOf(WorldStreetMap, OsmAndHd, WorldTopoMap)
