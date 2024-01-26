package com.peterlaurence.trekme.core.map.domain.repository

import com.peterlaurence.trekme.core.map.domain.models.Map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * This repository holds:
 *
 * * the [Map] list
 * * the [Map] that should be used when navigating to the feature showing the map.
 * * the [Map] that should be displayed when navigating to the map settings
 *
 * Invariants:
 * - As of v4.x.x, all mutable properties of a [Map] is backed by a MutableStatFlow. Consequently,
 *   a [Map] instance lives for the life duration of the application. No new [Map] is created from
 *   a copy of a previous instance (which would then become stale).
 *   There is an exception though: because the [Map]'s calibration data is stored using immutable
 *   properties, changing map calibration requires creating a new [Map] instance.
 *   However, this feature has been disabled. In the future, if this features is re-enabled, it
 *   should be done while preserving this invariant.
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
    fun getMap(mapId: UUID): Map? {
        return when (val mapListState = _mapListFlow.value) {
            Loading -> null
            is MapList -> mapListState.mapList.firstOrNull { it.id == mapId }
        }
    }

    fun deleteMap(map: Map) {
        val mapListState = _mapListFlow.value as? MapList ?: return
        _mapListFlow.value = MapList(mapListState.mapList - map)
    }

    /**
     * For situations when we need to get the list of maps at the time of the call, and we don't
     * need to react on map list changes.
     */
    fun getCurrentMapList(): List<Map> {
        return (_mapListFlow.value as? MapList)?.mapList ?: emptyList()
    }

    /**
     * Indicate that maps are currently being (re)loaded.
     */
    fun mapsLoading() {
        _mapListFlow.value = Loading
    }

    fun addMaps(maps: List<Map>) {
        /* Protect against duplicates */
        val uniqueMaps = maps.distinctBy { it.id }
        when (val mapListState = _mapListFlow.value) {
            Loading -> _mapListFlow.value = MapList(uniqueMaps)
            is MapList -> {
                _mapListFlow.value = MapList(
                    mapListState.mapList + uniqueMaps.filter { it !in mapListState.mapList }
                )
            }
        }
    }

    fun getCurrentMap(): Map? = _mapFlow.value

    fun setCurrentMap(map: Map) {
        _mapFlow.value = map
    }

    fun setSettingsMap(map: Map) {
        _settingsMapFlow.value = map
    }

    sealed interface MapListState
    data class MapList(val mapList: List<Map>) : MapListState
    object Loading : MapListState
}