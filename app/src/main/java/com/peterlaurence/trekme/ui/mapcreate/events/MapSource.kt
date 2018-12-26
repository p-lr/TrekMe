package com.peterlaurence.trekme.ui.mapcreate.events

import com.peterlaurence.trekme.core.mapsource.MapSource

data class MapSourceSelectedEvent(val mapSource: MapSource)

data class MapSourceSettingsEvent(val mapSource: MapSource)