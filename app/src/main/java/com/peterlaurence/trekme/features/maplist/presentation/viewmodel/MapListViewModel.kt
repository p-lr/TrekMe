package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.interactors.DeleteMapInteractor
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import com.peterlaurence.trekme.core.repositories.onboarding.OnBoardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val onBoardingRepository: OnBoardingRepository,
    private val deleteMapInteractor: DeleteMapInteractor
) : ViewModel() {

    /**
     * This state mirrors the MapListState from [MapRepository], with the difference that a
     * [MapStub] has additional view-related properties.
     */
    private val _mapListState: MutableState<MapListState> = mutableStateOf(MapListState.Loading)
    val mapListState: State<MapListState> = _mapListState

    init {
        viewModelScope.launch {
            mapRepository.mapListFlow.collect { mapListState ->
                when (mapListState) {
                    MapRepository.Loading -> Loading
                    is MapRepository.MapList -> {
                        val favList = settings.getFavoriteMapIds().first()
                        updateMapListInFragment(mapListState.mapList, favList)
                    }
                }
            }
        }
    }

    fun setMap(mapId: Int) {
        val map = mapRepository.getMap(mapId) ?: return

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
        val state = _mapListState.value
        if (state is MapListState.MapList) {
            val stub = state.mapList.firstOrNull { it.mapId == mapId }
            stub?.apply {
                isFavorite = !isFavorite
            }

            val ids = state.mapList.filter { it.isFavorite }.map { it.mapId }
            val mapList = mapRepository.getCurrentMapList()
            updateMapListInFragment(mapList, ids)

            /* Remember this setting */
            viewModelScope.launch {
                settings.setFavoriteMapIds(ids)
            }
        }
    }

    fun deleteMap(mapId: Int) {
        val map = mapRepository.getMap(mapId)
        if (map != null) {
            viewModelScope.launch {
                deleteMapInteractor.deleteMap(map)
            }
        }
    }

    fun onMapSettings(mapId: Int) {
        val map = mapRepository.getMap(mapId) ?: return
        mapRepository.setSettingsMap(map)
    }

    fun onNavigateToMapCreate(showOnBoarding: Boolean) {
        onBoardingRepository.setMapCreateOnBoarding(flag = showOnBoarding)
    }

    private fun updateMapListInFragment(mapList: List<Map>, favoriteMapIds: List<Int>) {
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

        _mapListState.value = MapListState.MapList(stubList)  // update with a copy of the list
    }

    private fun Map.toMapStub(): MapStub {
        return MapStub(id).apply {
            title = name
            image = this@toMapStub.thumbnailImage
        }
    }
}

sealed interface MapListState {
    object Loading : MapListState
    data class MapList(val mapList: List<MapStub>) : MapListState
}

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