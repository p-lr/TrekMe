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
import com.peterlaurence.trekme.util.UnzipProgressionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MapImportViewModel : ViewModel() {
    private val viewModels = MutableLiveData<List<ItemViewModel>>()

    private var viewModelsMap: kotlin.collections.Map<Int, ItemViewModel> = mapOf()

    fun unarchiveAsync(mapArchive: MapArchive) {
        viewModelScope.unarchive(mapArchive, object : UnzipProgressionListener {

            val viewModel = viewModelsMap[mapArchive.id]

            override fun onProgress(p: Int) {
                viewModelScope.launch {
                    viewModel?.item?.onProgress(p)
                }
            }

            /**
             * Import the extracted map.
             * For instance, only support extraction of [Map.MapOrigin.VIPS] maps.
             */
            override fun onUnzipFinished(outputDirectory: File) {

                MapImporter.importFromFile(outputDirectory, Map.MapOrigin.VIPS,
                        object : MapImporter.MapImportListener {
                            override fun onMapImported(map: Map, status: MapImporter.MapParserStatus) {
                                viewModelScope.launch {
                                    viewModel?.item?.onMapImported(map, status)
                                }
                            }

                            override fun onMapImportError(e: MapImporter.MapParseException?) {
                                // TODO : show an error message that something went wrong and send an event.
                            }
                        })

                viewModelScope.launch {
                    viewModel?.item?.onUnzipFinished()
                }
            }

            override fun onUnzipError() {
                viewModelScope.launch {
                    viewModel?.item?.onUnzipError()
                }
            }
        })
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
    }
}