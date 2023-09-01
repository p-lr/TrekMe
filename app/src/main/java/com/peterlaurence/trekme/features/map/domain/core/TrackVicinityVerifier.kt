package com.peterlaurence.trekme.features.map.domain.core

fun interface TrackVicinityVerifier {
    suspend fun isInVicinity(latitude: Double, longitude: Double, thresholdInMeters: Int): Boolean
}