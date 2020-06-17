package com.peterlaurence.trekme.viewmodel.record

import android.app.Application
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.service.LocationService
import com.peterlaurence.trekme.ui.dialogs.MapSelectedEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

/**
 * The business logic for importing a recording (gpx file) into a map.
 *
 * @author P.Laurence on 16/04/20
 */
class RecordViewModel @ViewModelInject constructor(
        private val trekMeContext: TrekMeContext,
        private val trackImporter: TrackImporter,
        private val app: Application
) : ViewModel() {
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
    fun onMapSelectedForRecord(event: MapSelectedEvent) {
        val map = MapLoader.getMap(event.mapId) ?: return

        val recording = recordingsSelected.firstOrNull() ?: return

        viewModelScope.launch {
            trackImporter.applyGpxFileToMapAsync(recording, map).let {
                /* Once done, all we want is to post an event */
                EventBus.getDefault().post(it)
            }
        }
    }

    fun startRecording() {
        val recordingsPath = trekMeContext.recordingsDir?.absolutePath ?: return
        val intent = Intent(app, LocationService::class.java)
        intent.putExtra(LocationService.RECORDINGS_PATH_ARG, recordingsPath)
        app.startService(intent)
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }
}