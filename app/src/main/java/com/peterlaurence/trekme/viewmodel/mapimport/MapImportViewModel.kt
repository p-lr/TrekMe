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
import com.peterlaurence.trekme.util.UnzipProgressionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File

class MapImportViewModel : ViewModel() {
    private val viewModels = MutableLiveData<List<ItemViewModel>>()

    private var viewModelsMap: kotlin.collections.Map<Int, ItemViewModel> = mapOf()

    data class UnzipProgressEvent(val archiveId: Int, val p: Int)
    data class UnzipErrorEvent(val archiveId: Int)
    data class UnzipFinishedEvent(val archiveId: Int, val outputFolder: File)

    init {
        EventBus.getDefault().register(this)
    }

    override fun onCleared() {
        super.onCleared()

        EventBus.getDefault().unregister(this)
    }

    /**
     * Launch the unzip of an archive. The [EventBus] is used to decouple the task from the actual
     * handler (which is this view model), to avoid memory leaks. This is because handler code has
     * reference on an [ItemViewModel] which may have reference on a [ItemPresenter], which as a
     * reference on a view holder.
     */
    fun unarchiveAsync(mapArchive: MapArchive) {
        viewModelScope.unarchive(mapArchive, object : UnzipProgressionListener {

            override fun onProgress(p: Int) {
                EventBus.getDefault().post(UnzipProgressEvent(mapArchive.id, p))
            }

            /**
             * Import the extracted map.
             * For instance, only support extraction of [Map.MapOrigin.VIPS] maps.
             */
            override fun onUnzipFinished(outputDirectory: File) {
                viewModelScope.launch {
                    val res = MapImporter.importFromFile(outputDirectory, Map.MapOrigin.VIPS)
                    EventBus.getDefault().post(MapImportedEvent(res.map, mapArchive.id, res.status))
                }

                EventBus.getDefault().post(UnzipFinishedEvent(mapArchive.id, outputDirectory))
            }

            override fun onUnzipError() {
                EventBus.getDefault().post(UnzipErrorEvent(mapArchive.id))
            }
        })
    }

    @Subscribe
    fun onUnzipProgress(event: UnzipProgressEvent) {
        val viewModel = viewModelsMap[event.archiveId]
        viewModelScope.launch {
            viewModel?.item?.onProgress(event.p)
        }
    }

    @Subscribe
    fun onMapImported(event: MapImportedEvent) {
        val viewModel = viewModelsMap[event.archiveId]
        viewModelScope.launch {
            viewModel?.item?.onMapImported(event.map, event.status)
        }
    }

    @Subscribe
    fun onUnzipFinished(event: UnzipFinishedEvent) {
        val viewModel = viewModelsMap[event.archiveId]
        viewModelScope.launch {
            viewModel?.item?.onUnzipFinished()
        }
    }

    @Subscribe
    fun onUnzipError(event: UnzipErrorEvent) {
        val viewModel = viewModelsMap[event.archiveId]
        viewModelScope.launch {
            viewModel?.item?.onUnzipError()
        }
    }

    fun updateMapArchiveList() {
        viewModelScope.launch {
            val archives = withContext(Dispatchers.Default) {
                MapLoader.getMapArchiveList()
            }

            viewModelsMap = archives.map {
                ItemViewModel(it)
            }.associateBy {
                it.mapArchive.id
            }

            viewModels.postValue(viewModelsMap.values.toList())
        }
    }

    fun getItemViewModelList(): LiveData<List<ItemViewModel>> {
        return viewModels
    }

    interface ItemPresenter {
        fun onProgress(progress: Int)
        fun onUnzipFinished()
        fun onUnzipError()
        fun onMapImported(map: Map, status: MapImporter.MapParserStatus)
    }

    class ItemViewModel(val mapArchive: MapArchive) {
        var item: ItemPresenter? = null

        fun bind(itemPresenter: ItemPresenter) {
            item = itemPresenter
        }

        fun unBind() {
            item = null
        }
    }
}