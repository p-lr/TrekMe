package com.peterlaurence.trekme.main.eventhandler

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadAlreadyRunning
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadEvent
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadFinished
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadPending
import com.peterlaurence.trekme.core.map.domain.models.MapDownloadStorageError
import com.peterlaurence.trekme.core.map.domain.models.MapNotRepairable
import com.peterlaurence.trekme.core.map.domain.models.MapUpdateFinished
import com.peterlaurence.trekme.core.map.domain.models.MapUpdatePending
import com.peterlaurence.trekme.core.map.domain.models.MissingApiError
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.navigation.wmtsDestination
import com.peterlaurence.trekme.main.navigation.navigateToMapList
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.compose.showSnackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MapDownloadEventHandler(
    downloadEvents: SharedFlow<MapDownloadEvent>,
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    context: Context,
    onGoToMap: (UUID) -> Unit,
    onShowWarningDialog: (WarningMessage) -> Unit
) {
    LaunchedEffectWithLifecycle(downloadEvents) { event ->
        when (event) {
            is MapDownloadFinished -> {
                if (wmtsDestination == navController.currentDestination?.route) {
                    navController.navigateToMapList()
                }
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        context.getString(R.string.service_download_finished),
                        actionLabel = context.getString(R.string.open_dialog),
                        isLong = true
                    )

                    if (result == SnackbarResult.ActionPerformed) {
                        onGoToMap(event.mapId)
                    }
                }
            }

            is MapUpdateFinished -> {
                val result = snackbarHostState.showSnackbar(
                    message = if (event.repairOnly) {
                        context.getString(R.string.service_repair_finished)
                    } else {
                        context.getString(R.string.service_update_finished)
                    },
                    actionLabel = context.getString(R.string.ok_dialog),
                    isLong = true
                )

                if (result == SnackbarResult.ActionPerformed) {
                    onGoToMap(event.mapId)
                }
            }

            MapDownloadAlreadyRunning -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_already_running
                        )
                    )
                )
            }

            MapDownloadStorageError -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_bad_storage
                        )
                    )
                )
            }

            MapNotRepairable -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_repair_error
                        )
                    )
                )
            }

            is MapDownloadPending, is MapUpdatePending -> {
                // Nothing particular to do, the service which fire those events already sends
                // notifications with the progression.
            }

            MissingApiError -> {
                onShowWarningDialog(
                    WarningMessage(
                        title = context.getString(R.string.warning_title),
                        msg = context.getString(
                            R.string.service_download_missing_api
                        )
                    )
                )
            }
        }
    }
}