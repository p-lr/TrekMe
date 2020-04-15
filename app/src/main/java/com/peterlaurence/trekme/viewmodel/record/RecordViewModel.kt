package com.peterlaurence.trekme.viewmodel.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.ui.record.components.events.MapSelectedForRecord
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 * When the user imports a recording (gpx file) into a map, the business logic is done in this
 * view-model.
 *
 * @author P.Laurence on 16/04/20
 */
class RecordViewModel : ViewModel() {
    private var recordingsSelected = listOf<File>()

    init {
        EventBus.getDefault().register(this)
    }

    fun setSelectedRecordings(recordings: List<File>) {
        recordingsSelected = recordings
    }

    /**
     * The business logic of parsing a GPX file.
     */
    @Subscribe
    fun onMapSelectedForRecord(event: MapSelectedForRecord) {
        val map = MapLoader.getMap(event.mapId) ?: return

        val recording = recordingsSelected.firstOrNull() ?: return

        viewModelScope.launch {
            TrackImporter.applyGpxFileToMapAsync(recording, map).let {
                /* Once done, all we want is to post an event */
                EventBus.getDefault().post(it)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}