package com.peterlaurence.trekme.core.elevation.domain.model

sealed class ElevationResult
data object Error : ElevationResult()
data object NonTrusted : ElevationResult()
data class TrustedElevations(val elevations: List<Double>) : ElevationResult()