package com.peterlaurence.trekme.core.excursion.data.model

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A file-based implementation of [ExcursionWaypoint]
 */
@Serializable
data class Waypoint(
    override val id: String,
    override val name: String,
    @SerialName("lat")
    override val latitude: Double,
    @SerialName("lon")
    override val longitude: Double,
    @SerialName("ele")
    override val elevation: Double?,
    override val comment: String,
    @SerialName("photos")
    override val photos: List<Photo>
) : ExcursionWaypoint