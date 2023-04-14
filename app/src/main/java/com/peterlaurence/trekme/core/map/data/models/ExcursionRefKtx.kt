package com.peterlaurence.trekme.core.map.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ExcursionRefKtx(
    val id: String,
    val name: String,
    val visible: Boolean,
    val color: String? = null
)