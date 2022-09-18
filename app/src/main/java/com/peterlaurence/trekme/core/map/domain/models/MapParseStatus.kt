package com.peterlaurence.trekme.core.map.domain.models

enum class MapParseStatus {
    NO_MAP, // no map could be created
    NEW_MAP, // a new map was successfully created
    EXISTING_MAP, // a map.json file was found
    UNKNOWN_MAP_ORIGIN
}
