package com.peterlaurence.trekme.main.eventhandler

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.main.MainActivity
import kotlinx.coroutines.launch
import java.util.*

class MapDownloadEventHandler(
    private val activity: MainActivity,
    private val lifecycle: Lifecycle,
    private val downloadRepository: DownloadRepository,
    private val onDownloadFinished: (mapId: UUID) -> Unit
) {

    init {
        lifecycle.coroutineScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                downloadRepository.downloadEvent.collect {
                    onMapDownloadEvent(it)
                }
            }
        }
    }

    private fun onMapDownloadEvent(event: MapDownloadEvent) = with(activity) {
        when (event) {
            is MapDownloadFinished -> {
                onDownloadFinished(event.mapId)
            }
            is MapDownloadStorageError -> showWarningDialog(
                getString(R.string.service_download_bad_storage),
                getString(R.string.warning_title),
                null
            )
            is MissingApiError -> showWarningDialog(
                getString(R.string.service_download_missing_api),
                getString(R.string.warning_title),
                null
            )
            is MapDownloadPending -> {
                // Nothing particular to do, the service which fire those events already sends
                // notifications with the progression.
            }
            is MapDownloadAlreadyRunning -> showWarningDialog(
                getString(R.string.service_download_already_running),
                getString(R.string.warning_title),
                null
            )
        }
    }
}