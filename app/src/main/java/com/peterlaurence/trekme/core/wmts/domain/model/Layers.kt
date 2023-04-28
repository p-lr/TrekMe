package com.peterlaurence.trekme.core.wmts.domain.model

sealed class Layer(open val id: String)

sealed class IgnLayer(override val id: String) : Layer(id)
object IgnClassic : IgnLayer(ignClassic)
object PlanIgnV2 : IgnLayer(ignPlanv2)
object Satellite : IgnLayer(ignSatellite)
object Road : IgnLayer(ignRoad)
object Slopes : IgnLayer(ignSlopes)
object Cadastre : IgnLayer(ignCadastre)

const val ignPlanv2 = "Plan IGN V2"
const val ignClassic = "Cartes IGN"
const val ignSatellite = "Photographies aériennes"
const val ignRoad = "Routes IGN"
const val ignSlopes = "Carte des pentes"
const val ignCadastre = "Parcelles cadastrales"

/* Primary IGN layers are layers which are rendered below overlay layers */
val ignLayersPrimary: List<IgnLayer> = listOf(IgnClassic, PlanIgnV2, Satellite)

/* Overlay layers can be drawn above primary layers (e.g routes, slopes, ..) */
val ignLayersOverlay: List<IgnLayer> = listOf(Slopes, Cadastre, Road)

sealed class OsmLayer(override val id: String) : Layer(id)
object WorldTopoMap : OsmLayer(osmTopo)
object WorldStreetMap : OsmLayer(osmStreet)
object OpenTopoMap : OsmLayer(openTopoMap)

const val osmTopo = "osmTopo"
const val osmStreet = "osmStreet"
const val openTopoMap = "openTopoMap"

/* All supported OSM layers */
val osmLayersPrimary: List<OsmLayer> = listOf(WorldStreetMap, WorldTopoMap, OpenTopoMap)
