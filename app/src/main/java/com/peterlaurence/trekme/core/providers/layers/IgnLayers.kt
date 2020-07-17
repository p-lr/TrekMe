package com.peterlaurence.trekme.core.providers.layers

sealed class Layer(open val publicName: String, open val realName: String)

sealed class IgnLayer(override val publicName: String, override val realName: String) : Layer(publicName, realName)
object ScanExpressStandard : IgnLayer(scanExpressStd, "GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD")
object IgnClassic : IgnLayer(ignClassic, "GEOGRAPHICALGRIDSYSTEMS.MAPS")
object Satellite : IgnLayer(satellite, "ORTHOIMAGERY.ORTHOPHOTOS")

private const val scanExpressStd = "Scan Express Standard"
private const val ignClassic = "Cartes IGN"
private const val satellite = "Photographies aériennes"

/* Workaround because we can't call SealedClass.values() without reflection :( */
val ignLayers = listOf(ignClassic, satellite, scanExpressStd)