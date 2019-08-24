package com.peterlaurence.trekme.viewmodel.maplist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.map.maploader.events.MapListUpdateEvent
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.model.map.MapModel
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import com.peterlaurence.trekme.ui.maplist.dialogs.ArchiveMapDialog
import com.peterlaurence.trekme.ui.maplist.events.ZipFinishedEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipProgressEvent
import com.peterlaurence.trekme.util.ZipTask
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 * The view-model intended to be used by the [MapListFragment], which is the only view where the
 * user can change of [Map].
 * So, all necessary model actions are taken in this view-model.
 */
class MapListViewModel: ViewModel() {
    private val maps = MutableLiveData<List<Map>>()

    init {
        EventBus.getDefault().register(this)

        updateMapListInFragment()
    }

    fun getMaps(): LiveData<List<Map>> {
        return maps
    }

    fun setMap(map: Map) {
        // 1- Sets the map to the main entity responsible for this
        MapModel.setCurrentMap(map)

        // 2- Remember this map in the case use wants to open TrekMe directly on this map
        viewModelScope.launch {
            Settings.setLastMapId(map.id)
        }
    }

    @Subscribe
    fun onMapListUpdateEvent(event: MapListUpdateEvent) {
        updateMapListInFragment()
    }

    private fun updateMapListInFragment() {
        val mapList = MapLoader.maps
        maps.postValue(mapList)
    }

    /**
     * Process a request to archive a [Map]. This is typically called from a [ArchiveMapDialog].
     *
     * @param event The [ArchiveMapDialog.SaveMapEvent] which contains the id of the [Map].
     */
    @Subscribe
    fun onSaveMapEvent(event: ArchiveMapDialog.SaveMapEvent) {
        val map = MapLoader.getMap(event.mapId) ?: return

        /* Effectively launch the archive task */
        map.zip(object : ZipTask.ZipProgressionListener {
            private val mapName = map.name
            private val mapId = map.id

            override fun fileListAcquired() {}

            override fun onProgress(p: Int) {
                EventBus.getDefault().post(ZipProgressEvent(p, mapName, mapId))
            }

            override fun onZipFinished(outputDirectory: File) {
                EventBus.getDefault().post(ZipFinishedEvent(mapId))
            }

            override fun onZipError() {}
        })
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}