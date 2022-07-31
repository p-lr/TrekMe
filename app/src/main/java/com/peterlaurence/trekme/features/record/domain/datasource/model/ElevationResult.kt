package com.peterlaurence.trekme.features.record.domain.datasource.model

sealed class ElevationResult
object Error : ElevationResult()
object NonTrusted : ElevationResult()
data class TrustedElevations(val elevations: List<Double>) : ElevationResult()