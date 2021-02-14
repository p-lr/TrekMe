package com.peterlaurence.trekme.viewmodel.maplist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This view-model is intended to be used by the [MapListFragment], which displays the list of [Map].
 * Handles map selection, map deletion, and setting a map as favorite.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
        private val settings: Settings,
        private val mapRepository: MapRepository,
        private val mapLoader: MapLoader
) : ViewModel() {
    private val _maps = MutableLiveData<List<Map>>()
    val maps: LiveData<List<Map>> = _maps

    init {
        viewModelScope.launch {
            mapLoader.mapListUpdateEventFlow.collect {
                val favList = settings.getFavoriteMapIds()
                updateMapListInFragment(favList)
            }
        }
    }

    fun setMap(map: Map) {
        // 1- Sets the map to the main entity responsible for this
        mapRepository.setCurrentMap(map)

        // 2- Remember this map in the case use wants to open TrekMe directly on this map
        settings.setLastMapId(map.id)
    }

    fun toggleFavorite(map: Map) {
        /* Toggle, then trigger a view refresh */
        map.isFavorite = !map.isFavorite
        val maps = mapLoader.maps
        val ids = maps.filter { it.isFavorite }.map { it.id }
        updateMapListInFragment(ids)

        /* Remember this setting */
        settings.setFavoriteMapIds(ids)
    }

    fun deleteMap(mapId: Int) {
        val map = mapLoader.getMap(mapId)
        if (map != null) {
            viewModelScope.launch {
                mapLoader.deleteMap(map)
            }
        }
    }

    private fun updateMapListInFragment(favoriteMapIds: List<Int>) {
        val mapList = mapLoader.maps

        /* Order map list with favorites first */
        val mapListSorted = if (favoriteMapIds.isNotEmpty()) {
            mapList.sortedByDescending { map ->
                if (favoriteMapIds.contains(map.id)) {
                    map.isFavorite = true
                    1
                } else -1
            }
        } else mapList

        _maps.postValue(mapListSorted)
    }
}