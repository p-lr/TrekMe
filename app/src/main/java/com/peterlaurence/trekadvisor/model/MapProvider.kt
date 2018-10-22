package com.peterlaurence.trekadvisor.model

import com.peterlaurence.trekadvisor.core.map.Map
import com.peterlaurence.trekadvisor.menu.mapview.MapViewFragment
import com.peterlaurence.trekadvisor.menu.maplist.MapSettingsFragment

/**
 * This singleton holds the references on:
 *
 * * the [Map] that should be used when navigating to the [MapViewFragment]
 * * the [Map] that should be displayed when navigating to the [MapSettingsFragment]
 */
object MapProvider {
    private var map: Map? = null
    private var settingsMap: Map? = null

    fun getCurrentMap(): Map? = map

    fun setCurrentMap(map: Map) {
        this.map = map
    }

    fun getSettingsMap() = settingsMap

    fun setSettingsMap(map: Map) {
        this.settingsMap = map
    }
}