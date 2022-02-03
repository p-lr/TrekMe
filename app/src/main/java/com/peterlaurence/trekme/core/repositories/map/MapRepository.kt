package com.peterlaurence.trekme.core.repositories.map

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.features.maplist.presentation.ui.MapSettingsFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This repository holds references on:
 *
 * * the [Map] that should be used when navigating to the feature showing the map.
 * * the [Map] that should be displayed when navigating to the [MapSettingsFragment]
 */
class MapRepository {
    private val _mapFlow = MutableStateFlow<Map?>(null)
    val mapFlow: StateFlow<Map?> = _mapFlow.asStateFlow()

    private val _settingsMapFlow = MutableStateFlow<Map?>(null)
    val settingsMapFlow: StateFlow<Map?> = _settingsMapFlow.asStateFlow()

    fun getCurrentMap(): Map? = _mapFlow.value

    fun setCurrentMap(map: Map) {
        _mapFlow.value = map
    }

    fun getSettingsMap() = _settingsMapFlow.value

    fun setSettingsMap(map: Map) {
        _settingsMapFlow.value = map
    }
}