package com.peterlaurence.trekme.ui.mapview.events

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson

data class TrackChangedEvent(val map: Map, val routeList: List<RouteGson.Route>, val addedMarkers: Int)