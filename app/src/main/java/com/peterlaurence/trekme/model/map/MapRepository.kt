package com.peterlaurence.trekme.model.map

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.ui.mapview.MapViewFragment
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment

/**
 * This repository holds references on:
 *
 * * the [Map] that should be used when navigating to the [MapViewFragment]
 * * the [Map] that should be displayed when navigating to the [MapSettingsFragment]
 */
class MapRepository {
    private var map: Map? = null
    private var settingsMap: Map? = null

    fun getCurrentMap(): Map? = map

    fun setCurrentMap(map: Map) {
        this.map = map
    }

    fun getSettingsMap() = settingsMap

    fun setSettingsMap(map: Map) {
        settingsMap = map
    }
}