package com.peterlaurence.trekme.features.record.domain.model

sealed class ElevationEvent
data class NoNetworkEvent(val internetOk: Boolean, val restApiOk: Boolean) : ElevationEvent()
object ElevationCorrectionErrorEvent : ElevationEvent()