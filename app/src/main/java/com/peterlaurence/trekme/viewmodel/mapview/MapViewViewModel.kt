package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.model.map.MapModel
import org.greenrobot.eventbus.EventBus

/**
 * The view model of the fragment which displays [Map]s.
 *
 * @author peterLaurence on 24/08/2019
 */
class MapViewViewModel : ViewModel() {
    private val mapLiveData = MutableLiveData<Map>()

    /**
     * Only update the map if its a new one.
     */
    fun updateMapIfNecessary(oldMap: Map?) {
        val map = MapModel.getCurrentMap()
        if (map != null) {
            if (oldMap != null && oldMap.equals(map)) {
                EventBus.getDefault().post(CalibrationMayChangedEvent(map))
            } else {
                /* The map changed */
                mapLiveData.postValue(map)
            }
        }
    }

    fun getMapLiveData(): LiveData<Map> = mapLiveData
}

data class CalibrationMayChangedEvent(val map: Map)