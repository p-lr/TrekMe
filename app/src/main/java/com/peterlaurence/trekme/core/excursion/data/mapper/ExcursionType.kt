package com.peterlaurence.trekme.core.excursion.data.mapper

import com.peterlaurence.trekme.core.excursion.data.model.Type
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType


fun Type.toDomain(): ExcursionType {
    return when (this) {
        Type.Hike -> ExcursionType.Hike
        Type.Running -> ExcursionType.Running
        Type.MountainBike -> ExcursionType.MountainBike
        Type.TravelBike -> ExcursionType.TravelBike
        Type.HorseRiding -> ExcursionType.HorseRiding
        Type.Aerial -> ExcursionType.Aerial
        Type.Nautical -> ExcursionType.Nautical
    }
}