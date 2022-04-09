package com.peterlaurence.trekme.core.map.data

import kotlinx.serialization.Serializable

@Serializable
data class RouteKtx (
    val markers: List<MarkerKtx> = listOf()
)

@Serializable
data class RouteInfoKtx(
    val id: String? = null,
    var name: String? = null,
    var visible: Boolean = true,
    var color: String? = null,
    var elevationTrusted: Boolean = false,
)