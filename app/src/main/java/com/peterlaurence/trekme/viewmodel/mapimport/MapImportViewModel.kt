package com.peterlaurence.trekme.viewmodel.mapimport

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.maparchiver.unarchive
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.events.MapImportedEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipErrorEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipFinishedEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipProgressionEvent
import com.peterlaurence.trekme.util.UnzipProgressionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import java.io.File

class MapImportViewModel: ViewModel() {
    private val mapArchiveList = MutableLiveData<List<MapArchive>>()

    fun unarchiveAsync(mapArchive: MapArchive) {
        viewModelScope.unarchive(mapArchive, object : UnzipProgressionListener {

            override fun onProgress(p: Int) {
                EventBus.getDefault().post(UnzipProgressionEvent(mapArchive.id, p))
            }

            /**
             * Import the extracted map.
             * For instance, only support extraction of [Map.MapOrigin.VIPS] maps.
             */
            override fun onUnzipFinished(outputDirectory: File) {

                MapImporter.importFromFile(outputDirectory, Map.MapOrigin.VIPS,
                        object : MapImporter.MapImportListener {
                            override fun onMapImported(map: Map, status: MapImporter.MapParserStatus) {
                                EventBus.getDefault().post(MapImportedEvent(map, mapArchive.id, status))
                            }

                            override fun onMapImportError(e: MapImporter.MapParseException?) {
                                // TODO : show an error message that something went wrong and send an event.
                            }
                        })

                EventBus.getDefault().post(UnzipFinishedEvent(mapArchive.id, outputDirectory))
            }

            override fun onUnzipError() {
                EventBus.getDefault().post(UnzipErrorEvent(mapArchive.id))
            }
        })
    }

    fun updateMapArchiveList() {
        viewModelScope.launch {
            val archives = withContext(Dispatchers.Default) {
                MapLoader.getMapArchiveList()
            }

            mapArchiveList.postValue(archives)
        }
    }

    fun getMapArchiveList(): LiveData<List<MapArchive>> {
        return mapArchiveList
    }
}