package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.interactors.DeleteMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.repositories.download.DownloadRepository
import com.peterlaurence.trekme.core.repositories.onboarding.OnBoardingRepository
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.features.maplist.presentation.model.DownloadItem
import com.peterlaurence.trekme.features.maplist.presentation.model.MapItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * This view-model is intended to be used by the map list UI, which displays the list of [Map].
 * Handles map selection, map deletion, and setting a map as favorite.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    private val settings: Settings,
    private val mapRepository: MapRepository,
    private val downloadRepository: DownloadRepository,
    private val onBoardingRepository: OnBoardingRepository,
    private val deleteMapInteractor: DeleteMapInteractor,
    private val app: Application,
) : ViewModel() {

    /**
     * This state mirrors the MapListState from [MapRepository], with the difference that a
     * [MapItem] has additional view-related properties.
     */
    private val _mapListState: MutableState<MapListState> =
        mutableStateOf(MapListState(emptyList(), true))
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

        viewModelScope.launch {
            downloadRepository.downloadEvent.collect { event ->
                when(event) {
                    is MapDownloadPending -> {
                        _mapListState.value.downloadItem.progress = event.progress
                        _mapListState.value = _mapListState.value.copy(isDownloadPending = true)
                    }
                    else -> { /* Nothing to do */ }
                }
            }
        }

        viewModelScope.launch {
            downloadRepository.started.collect {
                _mapListState.value = _mapListState.value.copy(isDownloadPending = it)
            }
        }
    }

    fun setMap(mapId: UUID) {
        val map = mapRepository.getMap(mapId) ?: return

        // 1- Sets the map to the main entity responsible for this
        mapRepository.setCurrentMap(map)

        // 2- Remember this map in the case use wants to open TrekMe directly on this map
        viewModelScope.launch {
            settings.setLastMapId(mapId)
        }
    }

    /**
     * Toggle the favorite flag on the [MapItem], then trigger UI update while saving favorites in
     * the settings.
     */
    fun toggleFavorite(mapId: UUID) {
        val state = _mapListState.value
        val mapItem = state.mapItems.firstOrNull { it.mapId == mapId }
        mapItem?.apply {
            isFavorite = !isFavorite
        }

        val ids = state.mapItems.filter { it.isFavorite }.map { it.mapId }
        val mapList = mapRepository.getCurrentMapList()
        updateMapListInFragment(mapList, ids)

        /* Remember this setting */
        viewModelScope.launch {
            settings.setFavoriteMapIds(ids)
        }
    }

    fun deleteMap(mapId: UUID) {
        val map = mapRepository.getMap(mapId)
        if (map != null) {
            viewModelScope.launch {
                deleteMapInteractor.deleteMap(map)
            }
        }
    }

    fun onMapSettings(mapId: UUID) {
        val map = mapRepository.getMap(mapId) ?: return
        mapRepository.setSettingsMap(map)
    }

    fun onNavigateToMapCreate(showOnBoarding: Boolean) {
        onBoardingRepository.setMapCreateOnBoarding(flag = showOnBoarding)
    }

    fun onCancelDownload() {
        val intent = Intent(app, DownloadService::class.java)
        intent.action = DownloadService.stopAction
        app.startService(intent)
    }

    private fun updateMapListInFragment(mapList: List<Map>, favoriteMapIds: List<UUID>) {
        /* Order map list with favorites first */
        val items = mapList.map { it.toMapStub() }.let {
            if (favoriteMapIds.isNotEmpty()) {
                it.sortedByDescending { stub ->
                    if (favoriteMapIds.contains(stub.mapId)) {
                        stub.isFavorite = true
                        1
                    } else -1
                }
            } else it
        }

        _mapListState.value = MapListState(items, false)  // update with a copy of the list
    }

    private fun Map.toMapStub(): MapItem {
        return MapItem(id).apply {
            title = name
            image = this@toMapStub.thumbnailImage
        }
    }
}

data class MapListState(
    val mapItems: List<MapItem>,
    val isMapListLoading: Boolean,
    val downloadItem: DownloadItem = DownloadItem(),
    val isDownloadPending: Boolean = false
)
