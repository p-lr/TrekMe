package com.peterlaurence.trekme.core.map.domain.models

sealed interface MapLicense
object FreeLicense : MapLicense
object ValidIgnLicense : MapLicense
object ValidWmtsLicense : MapLicense
data class ErrorIgnLicense(val map: Map) : MapLicense
data class ErrorWmtsLicense(val map: Map) : MapLicense