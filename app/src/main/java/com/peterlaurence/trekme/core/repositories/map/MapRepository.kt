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
    private val _mapListFlow = MutableStateFlow<MapListState>(Loading)
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
        return when (val mapListState = _mapListFlow.value) {
            Loading -> null
            is MapList -> mapListState.mapList.firstOrNull { it.id == mapId }
        }
    }

    fun deleteMap(map: Map) {
        val mapListState = _mapListFlow.value as? MapList ?: return
        _mapListFlow.value = MapList(mapListState.mapList - map)
    }

    fun notifyUpdate(oldMap: Map, newMap: Map) {
        val mapListState = _mapListFlow.value as? MapList ?: return

        _mapListFlow.value = MapList(
            mapListState.mapList.indexOf(oldMap).let { i ->
                if (i >= 0) {
                    mapListState.mapList.toMutableList().apply {
                        set(i, newMap)
                    }
                } else mapListState.mapList
            }
        )

        /* If necessary, update the current map */
        if (oldMap == _mapFlow.value) {
            _mapFlow.value = newMap
        }

        /* If necessary, update the settings map */
        if (oldMap == _settingsMapFlow.value) {
            _settingsMapFlow.value = newMap
        }
    }

    /**
     * For situations when we need to get the list of maps at the time of the call, and we don't
     * need to react on map list changes.
     */
    fun getCurrentMapList() : List<Map> {
        return (_mapListFlow.value as? MapList)?.mapList ?: emptyList()
    }

    /**
     * Indicate that maps are currently being (re)loaded.
     */
    fun mapsLoading() {
        _mapListFlow.value = Loading
    }

    fun addMaps(maps: List<Map>) {
        when (val mapListState = _mapListFlow.value) {
            Loading -> _mapListFlow.value = MapList(maps)
            is MapList -> {
                _mapListFlow.value = MapList(
                    mapListState.mapList + maps.filter { it !in mapListState.mapList }
                )
            }
        }
    }

    fun getCurrentMap(): Map? = _mapFlow.value

    fun setCurrentMap(map: Map) {
        _mapFlow.value = map
    }

    fun getSettingsMap() = _settingsMapFlow.value

    fun setSettingsMap(map: Map) {
        _settingsMapFlow.value = map
    }

    sealed interface MapListState
    data class MapList(val mapList: List<Map>) : MapListState
    object Loading : MapListState
}