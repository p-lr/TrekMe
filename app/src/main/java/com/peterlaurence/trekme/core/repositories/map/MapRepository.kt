package com.peterlaurence.trekme.core.repositories.map

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.ui.mapview.MapViewFragment
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This repository holds references on:
 *
 * * the [Map] that should be used when navigating to the [MapViewFragment]
 * * the [Map] that should be displayed when navigating to the [MapSettingsFragment]
 */
class MapRepository {
    private val _mapFlow = MutableStateFlow<Map?>(null)
    val mapFlow: StateFlow<Map?> = _mapFlow.asStateFlow()

    private var settingsMap: Map? = null

    fun getCurrentMap(): Map? = _mapFlow.value

    fun setCurrentMap(map: Map) {
        _mapFlow.value = map
    }

    fun getSettingsMap() = settingsMap

    fun setSettingsMap(map: Map) {
        settingsMap = map
    }
}