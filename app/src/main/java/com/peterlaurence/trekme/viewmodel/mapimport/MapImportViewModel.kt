package com.peterlaurence.trekme.viewmodel.mapimport

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maparchiver.unarchive
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.ui.events.MapImportedEvent
import com.peterlaurence.trekme.util.UnzipProgressionListener
import com.peterlaurence.trekme.viewmodel.mapimport.MapImportViewModel.ItemPresenter
import com.peterlaurence.trekme.viewmodel.mapimport.MapImportViewModel.ItemViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.File
import java.io.InputStream

/**
 * This view-model manages [ItemViewModel]s, which are wrappers around [DocumentFile]s.
 * The view supplies the list of [DocumentFile]s when the user selects a directory.
 * An [ItemViewModel] can be bound to an [ItemPresenter]. From this view-model standpoint, it's only
 * an interface. The view binds and unbinds concrete implementations of [ItemPresenter]
 * (a RecyclerView binds and unbinds ViewHolders).
 * For example, when this view-model gets notified by a background task of a progress for an
 * extraction of an [ItemViewModel], the corresponding [ItemPresenter] bound instance can be called
 * if it exists.
 */
class MapImportViewModel : ViewModel() {
    private val viewModels = MutableLiveData<List<ItemViewModel>>()

    private var viewModelsMap = mapOf<Int, ItemViewModel>()

    data class UnzipProgressEvent(val archiveId: Int, val p: Int)
    data class UnzipErrorEvent(val archiveId: Int)
    data class UnzipFinishedEvent(val archiveId: Int, val outputFolder: File)

    init {
        EventBus.getDefault().register(this)
    }

    /**
     * The view gives the view-model the list of [DocumentFile].
     * Then we prepare the model and notify the view with the corresponding list of [ItemViewModel].
     */
    fun updateUriList(docs: List<DocumentFile>) {
        viewModelsMap = docs.map {
            ItemViewModel(it)
        }.associateBy {
            it.id
        }

        viewModels.postValue(viewModelsMap.values.toList())
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
    fun unarchiveAsync(inputStream: InputStream, item: ItemViewModel) {
        val outputFolder = TrekMeContext.importedDir ?: return

        /* If the document has no name, give it one */
        val name = item.docFile.name ?: "mapImported"

        viewModelScope.unarchive(inputStream, outputFolder, name, item.docFile.length(),
                object : UnzipProgressionListener {
                    override fun onProgress(p: Int) {
                        EventBus.getDefault().post(UnzipProgressEvent(item.id, p))
                    }

                    /**
                     * Import the extracted map.
                     * For instance, only support extraction of [Map.MapOrigin.VIPS] maps.
                     */
                    override fun onUnzipFinished(outputDirectory: File) {
                        viewModelScope.launch {
                            val res = MapImporter.importFromFile(outputDirectory, Map.MapOrigin.VIPS)
                            EventBus.getDefault().post(MapImportedEvent(res.map, item.id, res.status))
                        }

                        EventBus.getDefault().post(UnzipFinishedEvent(item.id, outputDirectory))
                    }

                    override fun onUnzipError() {
                        EventBus.getDefault().post(UnzipErrorEvent(item.id))
                    }
                }
        )
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

    fun getItemViewModelList(): LiveData<List<ItemViewModel>> {
        return viewModels
    }

    interface ItemPresenter {
        fun onProgress(progress: Int)
        fun onUnzipFinished()
        fun onUnzipError()
        fun onMapImported(map: Map, status: MapImporter.MapParserStatus)
    }

    class ItemViewModel(val docFile: DocumentFile) {
        var item: ItemPresenter? = null
        val id: Int = docFile.uri.hashCode()

        fun bind(itemPresenter: ItemPresenter) {
            item = itemPresenter
        }

        fun unBind() {
            item = null
        }
    }
}