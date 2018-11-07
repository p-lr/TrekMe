package com.peterlaurence.trekadvisor.menu.mapview.events

import com.peterlaurence.trekadvisor.core.map.Map
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson

data class TrackChangedEvent(val map: Map, val routeList: List<RouteGson.Route>, val addedMarkers: Boolean)