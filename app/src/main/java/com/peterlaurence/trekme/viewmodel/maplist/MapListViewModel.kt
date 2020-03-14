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
import com.peterlaurence.trekme.ui.maplist.events.ZipError
import com.peterlaurence.trekme.ui.maplist.events.ZipEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipFinishedEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipProgressEvent
import com.peterlaurence.trekme.util.ZipTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 * The view-model intended to be used by the [MapListFragment], which is the only view where the
 * user can change of [Map].
 * So, all necessary model actions are taken in this view-model.
 */
class MapListViewModel : ViewModel() {
    private val _maps = MutableLiveData<List<Map>>()
    val maps: LiveData<List<Map>> = _maps

    private val _zipEvents = MutableLiveData<ZipEvent>()
    val zipEvents: LiveData<ZipEvent> = _zipEvents

    init {
        EventBus.getDefault().register(this)

        updateMapListInFragment()
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
        _maps.postValue(mapList)
    }

    /**
     * Start zipping a map.
     * Internally uses a [Flow] which only emits distinct events.
     */
    fun startZipTask(mapId: Int) = viewModelScope.launch {
        zipProgressFlow(mapId).distinctUntilChanged().collect {
            _zipEvents.value = it
        }
    }


    @ExperimentalCoroutinesApi
    fun zipProgressFlow(mapId: Int): Flow<ZipEvent> = callbackFlow {
        val map = MapLoader.getMap(mapId) ?: return@callbackFlow

        val callback = object : ZipTask.ZipProgressionListener {
            private val mapName = map.name

            override fun fileListAcquired() {}

            override fun onProgress(p: Int) {
                offer(ZipProgressEvent(p, mapName, mapId))
            }

            override fun onZipFinished(outputDirectory: File) {
                offer(ZipFinishedEvent(mapId))
                channel.close()
            }

            override fun onZipError() {
                offer(ZipError)
                channel.close()
            }
        }
        map.zip(callback)
        awaitClose()
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}