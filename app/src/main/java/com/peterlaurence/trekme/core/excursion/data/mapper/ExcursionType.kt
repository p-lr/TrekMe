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
        Type.MotorisedVehicle -> ExcursionType.MotorisedVehicle
    }
}

fun ExcursionType.toData(): Type {
    return when (this) {
        ExcursionType.Hike -> Type.Hike
        ExcursionType.Running -> Type.Running
        ExcursionType.MountainBike -> Type.MountainBike
        ExcursionType.TravelBike -> Type.TravelBike
        ExcursionType.HorseRiding -> Type.HorseRiding
        ExcursionType.Aerial -> Type.Aerial
        ExcursionType.Nautical -> Type.Nautical
        ExcursionType.MotorisedVehicle -> Type.MotorisedVehicle
    }
}