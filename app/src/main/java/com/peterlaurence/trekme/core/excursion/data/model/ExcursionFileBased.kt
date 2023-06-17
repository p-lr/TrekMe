package com.peterlaurence.trekme.core.excursion.data.model

import com.peterlaurence.trekme.core.excursion.data.mapper.toDomain
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionPhoto
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

class ExcursionFileBased(
    val root: File,
    val config: ExcursionConfig
) : Excursion {
    override val id: String
        get() = config.id
    override val title: String
        get() = config.title
    override val type: ExcursionType
        get() = config.type.toDomain()
    override val description: String
        get() = config.description
    override val photos: List<ExcursionPhoto>
        get() = config.photos
}

@Serializable
data class ExcursionConfig(
    val id: String,
    val title: String,
    @SerialName("description")
    val description: String,
    val type: Type,
    @SerialName("photos")
    val photos: List<Photo> = emptyList()
)

@Serializable
enum class Type {
    @SerialName("hike")
    Hike,

    @SerialName("running")
    Running,

    @SerialName("mountain-bike")
    MountainBike,

    @SerialName("travel-bike")
    TravelBike,

    @SerialName("horse-riding")
    HorseRiding,

    @SerialName("aerial")
    Aerial,

    @SerialName("nautical")
    Nautical,

    @SerialName("motorised-vehicle")
    MotorisedVehicle
}
