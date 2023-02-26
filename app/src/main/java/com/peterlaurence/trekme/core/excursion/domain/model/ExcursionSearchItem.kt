package com.peterlaurence.trekme.core.excursion.domain.model


data class ExcursionSearchItem(
    val id: String,
    val title: String,
    val type: ExcursionType,
    val description: String = ""
)
