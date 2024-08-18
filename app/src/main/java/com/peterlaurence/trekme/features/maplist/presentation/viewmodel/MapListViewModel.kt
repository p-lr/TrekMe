package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.interactors.DeleteMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.SetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadPending
import com.peterlaurence.trekme.core.map.domain.models.NewDownloadSpec
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.common.domain.repositories.OnBoardingRepository
import com.peterlaurence.trekme.features.mapcreate.app.service.download.DownloadService
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.features.maplist.presentation.model.MapItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * This view-model is intended to be used by the map list UI, which displays the list of [Map].
 * Handles map selection, map deletion, and setting a map as favorite.
 */
@HiltViewModel
class MapListViewModel @Inject constructor(
    private val settings: Settings,
    private val mapRepository: MapRepository,
    private val setMapInteractor: SetMapInteractor,
    private val downloadRepository: DownloadRepository,
    private val onBoardingRepository: OnBoardingRepository,
    private val deleteMapInteractor: DeleteMapInteractor,
    private val app: Application
) : ViewModel() {

    /**
     * This state mirrors the MapListState from [MapRepository], with the difference that a
     * [MapItem] has additional view-related properties.
     */
    private val _mapListState = MutableStateFlow(MapListState(emptyList(), true))
    val mapListState: StateFlow<MapListState> = _mapListState.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState = _downloadState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(mapRepository.mapListFlow, downloadRepository.status) { mapListState, downloadStatus ->
                val idDownloading = if (downloadStatus is DownloadRepository.DownloadingNewMap) {
                    downloadStatus.mapId
                } else null

                when (mapListState) {
                    MapRepository.Loading -> Loading
                    is MapRepository.MapList -> {
                        val favList = settings.getFavoriteMapIds().first()
                        updateMapList(mapListState.mapList, favList, idDownloading)
                    }
                }
            }.launchIn(this)
        }

        viewModelScope.launch {
            downloadRepository.downloadEvent.collect { event ->
                if (event is MapDownloadPending) {
                    _downloadState.update {
                        it.copy(downloadProgress = event.progress)
                    }
                }
            }
        }

        viewModelScope.launch {
            downloadRepository.status.collect { status ->
                _downloadState.update {
                    it.copy(
                        isDownloadPending = status is DownloadRepository.DownloadingNewMap
                    )
                }
            }
        }
    }

    fun setMap(mapId: UUID) = viewModelScope.launch {
        setMapInteractor.setMap(mapId)
    }

    /**
     * Toggle the favorite flag on the [MapItem], then trigger UI update while saving favorites in
     * the settings.
     */
    fun toggleFavorite(mapId: UUID) {
        val state = _mapListState.value

        val mapItems = state.mapItems.map {
            if (it.mapId == mapId) {
                it.copy(isFavorite = !it.isFavorite)
            } else it
        }
        _mapListState.value = state.copy(mapItems = mapItems.sortedByDescending { it.isFavorite })

        val ids = _mapListState.value.mapItems.filter { it.isFavorite }.map { it.mapId }

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

    /**
     * Order maps so that favorite appear first, then order by name.
     */
    private fun updateMapList(mapList: List<Map>, favoriteMapIds: List<UUID>, idDownloading: UUID?) {
        /* Order map list with favorites first */
        val items = mapList
            .filter { it.id != idDownloading }
            .sortedBy { it.name.value.lowercase() }
            .map {
                val isFavorite = favoriteMapIds.isNotEmpty() && favoriteMapIds.contains(it.id)
                it.toMapItem(isFavorite)
            }
            .sortedByDescending { it.isFavorite }

        _mapListState.update {
            it.copy(
                mapItems = items, // update with a copy of the list
                isMapListInitializing = false,
            )
        }
    }

    private fun Map.toMapItem(isFavorite: Boolean): MapItem {
        return MapItem(id, titleFlow = name, isFavorite = isFavorite, image = thumbnail)
    }
}

data class MapListState(
    val mapItems: List<MapItem>,
    val isMapListInitializing: Boolean,
)

data class DownloadState(
    val downloadProgress: Int = 0,
    val isDownloadPending: Boolean = false
)
