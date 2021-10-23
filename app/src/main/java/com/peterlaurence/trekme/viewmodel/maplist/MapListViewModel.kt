package com.peterlaurence.trekme.viewmodel.maplist

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.repositories.onboarding.OnBoardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This view-model is intended to be used by the map list UI, which displays the list of [Map].
 * Handles map selection, map deletion, and setting a map as favorite.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
        private val settings: Settings,
        private val mapRepository: MapRepository,
        private val mapLoader: MapLoader,
        private val onBoardingRepository: OnBoardingRepository
) : ViewModel() {

    private val _mapState: MutableState<MapListState> = mutableStateOf(Loading, policy = structuralEqualityPolicy())
    val mapState: State<MapListState> = _mapState

    init {
        viewModelScope.launch {
            mapLoader.mapListUpdateEventFlow.collect {
                val favList = settings.getFavoriteMapIds().first()
                updateMapListInFragment(favList)
            }
        }
    }

    fun setMap(mapId: Int) {
        val map = mapLoader.getMap(mapId) ?: return

        // 1- Sets the map to the main entity responsible for this
        mapRepository.setCurrentMap(map)

        // 2- Remember this map in the case use wants to open TrekMe directly on this map
        viewModelScope.launch {
            settings.setLastMapId(mapId)
        }
    }

    /**
     * Toggle the favorite flag on the [MapStub], then trigger UI update while saving favorites in
     * the settings.
     */
    fun toggleFavorite(mapId: Int) {
        val state = _mapState.value
        if (state is MapList) {
            val stub = state.mapList.firstOrNull { it.mapId == mapId }
            stub?.apply {
                isFavorite = !isFavorite
            }

            val ids = state.mapList.filter { it.isFavorite }.map { it.mapId }
            updateMapListInFragment(ids)

            /* Remember this setting */
            viewModelScope.launch {
                settings.setFavoriteMapIds(ids)
            }
        }
    }

    fun deleteMap(mapId: Int) {
        val map = mapLoader.getMap(mapId)
        if (map != null) {
            viewModelScope.launch {
                mapLoader.deleteMap(map)
            }
        }
    }

    fun onMapSettings(mapId: Int) {
        val map = mapLoader.getMap(mapId) ?: return
        mapRepository.setSettingsMap(map)
    }

    fun onNavigateToMapCreate(showOnBoarding: Boolean) {
        onBoardingRepository.setMapCreateOnBoarding(flag = showOnBoarding)
    }

    private fun updateMapListInFragment(favoriteMapIds: List<Int>) {
        val mapList = mapLoader.maps

        /* Order map list with favorites first */
        val stubList = mapList.map { it.toMapStub() }.let {
            if (favoriteMapIds.isNotEmpty()) {
                it.sortedByDescending { stub ->
                    if (favoriteMapIds.contains(stub.mapId)) {
                        stub.isFavorite = true
                        1
                    } else -1
                }
            } else it
        }

        _mapState.value = MapList(stubList)  // update with a copy of the list
    }

    private fun Map.toMapStub(): MapStub {
        return MapStub(id).apply {
            title = name
            image = this@toMapStub.image
        }
    }
}

sealed interface MapListState
object Loading : MapListState
data class MapList(val mapList: List<MapStub>) : MapListState

class MapStub(val mapId: Int) {
    var isFavorite: Boolean by mutableStateOf(false)
    var title: String by mutableStateOf("")
    var image: Bitmap? by mutableStateOf(null)

    override fun equals(other: Any?): Boolean {
        if (other !is MapStub) return false
        return mapId == other.mapId
                && isFavorite == other.isFavorite
                && title == other.title
                && image == other.image
    }

    override fun hashCode(): Int {
        var result = mapId
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        return result
    }
}