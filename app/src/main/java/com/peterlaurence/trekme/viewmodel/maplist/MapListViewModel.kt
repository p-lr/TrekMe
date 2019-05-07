package com.peterlaurence.trekme.viewmodel.maplist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.map.maploader.events.MapListUpdateEvent
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.model.map.MapProvider
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

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
        MapProvider.setCurrentMap(map)

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

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}