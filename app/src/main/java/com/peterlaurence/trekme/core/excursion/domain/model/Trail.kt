package com.peterlaurence.trekme.core.excursion.domain.model

data class TrailSearchItem(
    val id: String,
    val ref: String?,
    val name: String?,
    val group: OsmTrailGroup?
)

enum class OsmTrailGroup {
    International,
    National,
    Regional,
    Local
}

interface TrailDetail {
    val id: String
    fun iteratePoints(block: (index: Int, x: Double, y: Double) -> Unit)
}