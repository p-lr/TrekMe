package com.peterlaurence.trekme.core.providers.layers

sealed class Layer(open val publicName: String, open val realName: String)

sealed class IgnLayer(override val publicName: String, override val realName: String) : Layer(publicName, realName)
object ScanExpressStandard : IgnLayer(scanExpressStd, "GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD")
object IgnClassic : IgnLayer(ignClassic, "GEOGRAPHICALGRIDSYSTEMS.MAPS")
object Satellite : IgnLayer(satellite, "ORTHOIMAGERY.ORTHOPHOTOS")

const val scanExpressStd = "Scan Express Standard"
const val ignClassic = "Cartes IGN"
const val satellite = "Photographies aériennes"

/* All supported layers */
val ignLayers: List<IgnLayer> = listOf(IgnClassic, Satellite)