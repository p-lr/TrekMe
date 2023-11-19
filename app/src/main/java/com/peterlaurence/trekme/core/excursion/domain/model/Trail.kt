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
    /**
     * For each segment, calls [block] with the index of the segment and the relative coordinates
     * of the point.
     */
    fun iteratePoints(block: (index: Int, x: Double, y: Double) -> Unit)
}

interface TrailDetailWithElevation {
    val id: String
    /**
     * For each segment, calls [block] with the index of the segment and the relative coordinates
     * and elevation of the point.
     */
    fun iteratePoints(block: (index: Int, x: Double, y: Double, elevation: Double) -> Unit)
}