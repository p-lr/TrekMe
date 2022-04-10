package com.peterlaurence.trekme.core.repositories.map

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.features.maplist.presentation.ui.MapSettingsFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This repository holds:
 *
 * * the [Map] list
 * * the [Map] that should be used when navigating to the feature showing the map.
 * * the [Map] that should be displayed when navigating to the [MapSettingsFragment]
 */
class MapRepository {
    private val _mapListFlow = MutableStateFlow<List<Map>>(listOf())
    val mapListFlow = _mapListFlow.asStateFlow()

    private val _mapFlow = MutableStateFlow<Map?>(null)
    val currentMapFlow: StateFlow<Map?> = _mapFlow.asStateFlow()

    private val _settingsMapFlow = MutableStateFlow<Map?>(null)
    val settingsMapFlow: StateFlow<Map?> = _settingsMapFlow.asStateFlow()

    /**
     * Get a [Map] from its id.
     *
     * @return the [Map] or `null` if the given id is unknown.
     */
    fun getMap(mapId: Int): Map? {
        return _mapListFlow.value.firstOrNull { it.id == mapId }
    }

    fun deleteMap(map: Map) {
        _mapListFlow.value = _mapListFlow.value - map
    }

    fun clearMaps() {
        _mapListFlow.value = listOf()
    }

    fun addMaps(maps: List<Map>) {
        _mapListFlow.value = _mapListFlow.value + maps.filter { it !in _mapListFlow.value }
    }

    fun getCurrentMap(): Map? = _mapFlow.value

    fun setCurrentMap(map: Map) {
        _mapFlow.value = map
    }

    fun getSettingsMap() = _settingsMapFlow.value

    fun setSettingsMap(map: Map) {
        _settingsMapFlow.value = map
    }
}