package com.peterlaurence.trekadvisor.core.mapsource

object MapSourceLoader {
    val supportedMapSource = MapSource.values()
}

enum class MapSource {
    IGN, OPEN_STREET_MAP
}