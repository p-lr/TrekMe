package com.peterlaurence.trekme.core.excursion.domain.model

enum class ExcursionCategory(val types: List<ExcursionType>) {
    OnFoot(listOf(ExcursionType.Hike, ExcursionType.Running)),
    Bike(listOf(ExcursionType.MountainBike, ExcursionType.TravelBike)),
    Horse(listOf(ExcursionType.HorseRiding)),
    Nautical(listOf(ExcursionType.Nautical)),
    Aerial(listOf(ExcursionType.Aerial)),
    Motorised(listOf(ExcursionType.MotorisedVehicle))
}