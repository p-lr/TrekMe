package com.peterlaurence.trekme.core.providers.layers

sealed class Layer(open val id: String, open val wmtsName: String)

sealed class IgnLayer(override val id: String, override val wmtsName: String) : Layer(id, wmtsName)
object ScanExpressStandard : IgnLayer(ignScanExpressStd, "GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD")
object IgnClassic : IgnLayer(ignClassic, "GEOGRAPHICALGRIDSYSTEMS.MAPS")
object PlanIgnV2 : IgnLayer(ignPlanv2, "GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2")
object Satellite : IgnLayer(ignSatellite, "ORTHOIMAGERY.ORTHOPHOTOS")

sealed class IgnLayerOverlay(override val id: String, override val wmtsName: String) : Layer(id, wmtsName)
object Road : IgnLayerOverlay(ignRoad, "TRANSPORTNETWORKS.ROADS")
object Slopes : IgnLayerOverlay(ignSlopes, "GEOGRAPHICALGRIDSYSTEMS.SLOPES.MOUNTAIN")
object Cadastre : IgnLayerOverlay(ignCadastre, "CADASTRALPARCELS.PARCELLAIRE_EXPRESS")

const val ignScanExpressStd = "Scan Express Standard"
const val ignPlanv2 = "Plan IGN V2"
const val ignClassic = "Cartes IGN"
const val ignSatellite = "Photographies aériennes"
const val ignRoad = "Routes IGN"
const val ignSlopes = "Carte des pentes"
const val ignCadastre = "Parcelles cadastrales"

/* Primary IGN layers are layers which are rendered below overlay layers */
val ignLayersPrimary: List<IgnLayer> = listOf(IgnClassic, PlanIgnV2, Satellite, ScanExpressStandard)

/* Overlay layers can be drawn above primary layers (e.g routes, slopes, ..) */
val ignLayersOverlay: List<IgnLayerOverlay> = listOf(Slopes, Cadastre, Road)

sealed class OsmLayer(override val id: String, override val wmtsName: String) : Layer(id, wmtsName)
object WorldTopoMap : OsmLayer(osmTopo, "World_Topo_Map")
object WorldStreetMap : OsmLayer(osmStreet, "World_Street_Map")
object OpenTopoMap : OsmLayer(openTopoMap, "OpenTopoMap")

const val osmTopo = "osmTopo"
const val osmStreet = "osmStreet"
const val openTopoMap = "openTopoMap"

/* All supported OSM layers */
val osmLayers: List<OsmLayer> = listOf(WorldStreetMap, WorldTopoMap, OpenTopoMap)

fun getLayer(id: String): Layer? {
    return when (id) {
        ignPlanv2 -> PlanIgnV2
        ignScanExpressStd -> ScanExpressStandard
        ignClassic -> IgnClassic
        ignSatellite -> Satellite
        osmTopo -> WorldTopoMap
        osmStreet -> WorldStreetMap
        openTopoMap -> OpenTopoMap
        else -> null
    }
}