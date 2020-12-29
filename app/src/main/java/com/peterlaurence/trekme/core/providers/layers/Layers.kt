package com.peterlaurence.trekme.core.providers.layers

sealed class Layer(open val id: String, open val wmtsName: String)

sealed class IgnLayer(override val id: String, override val wmtsName: String) : Layer(id, wmtsName)
object ScanExpressStandard : IgnLayer(ignScanExpressStd, "GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD")
object IgnClassic : IgnLayer(ignClassic, "GEOGRAPHICALGRIDSYSTEMS.MAPS")
object Satellite : IgnLayer(ignSatellite, "ORTHOIMAGERY.ORTHOPHOTOS")

const val ignScanExpressStd = "Scan Express Standard"
const val ignClassic = "Cartes IGN"
const val ignSatellite = "Photographies aériennes"

/* All supported IGN layers */
val ignLayers: List<IgnLayer> = listOf(IgnClassic, Satellite, ScanExpressStandard)

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
        ignScanExpressStd -> ScanExpressStandard
        ignClassic -> IgnClassic
        ignSatellite -> Satellite
        osmTopo -> WorldTopoMap
        osmStreet -> WorldStreetMap
        openTopoMap -> OpenTopoMap
        else -> null
    }
}