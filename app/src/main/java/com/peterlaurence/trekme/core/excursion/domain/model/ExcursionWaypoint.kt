package com.peterlaurence.trekme.core.excursion.domain.model

interface ExcursionWaypoint {
    val id: String
    val name: String
    val latitude: Double
    val longitude: Double
    val elevation: Double?
    val comment: String
    val photos: List<ExcursionPhoto>
    /**
     * Color in the format "#AARRGGBB".
     * By design, excursion waypoints don't have a default color. By default, they take the color
     * of the corresponding excursion. When they have a color set, it takes precedence over the
     * excursion's color.
     */
    val color: String?
}