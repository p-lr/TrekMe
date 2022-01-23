package com.peterlaurence.trekme.core.map

sealed interface MapLicense
object FreeLicense : MapLicense
object ValidIgnLicense : MapLicense
data class ErrorIgnLicense(val map: Map) : MapLicense