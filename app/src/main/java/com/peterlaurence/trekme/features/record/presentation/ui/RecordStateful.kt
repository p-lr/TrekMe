package com.peterlaurence.trekme.features.record.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.ui.components.GpxRecordListStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordTopAppbar
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle


@Composable
fun RecordStateful(
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    onElevationGraphClick: (RecordingData) -> Unit,
    onGoToTrailSearchClick: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val deletionFailedMsg = stringResource(id = R.string.files_could_not_be_deleted)
    val geoRecordAddMsg = stringResource(id = R.string.track_is_being_added)
    val geoRecordOutOfBoundsMsg = stringResource(id = R.string.import_result_out_of_bounds)
    val geoRecordAdErrorMsg = stringResource(id = R.string.track_add_error)
    val geoRecordRecover = stringResource(id = R.string.track_is_being_restored)

    var isMultiSelectionMode by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffectWithLifecycle(flow = statViewModel.recordingDeletionFailureFlow) {
        snackbarHostState.showSnackbar(message = deletionFailedMsg)
    }

    LaunchedEffectWithLifecycle(recordViewModel.geoRecordImportResultFlow) { result ->
        when (result) {
            is GeoRecordImportResult.GeoRecordImportOk ->
                /* Tell the user that the track will be shortly available in the map */
                snackbarHostState.showSnackbar(geoRecordAddMsg)

            GeoRecordImportResult.GeoRecordImportError ->
                /* Tell the user that an error occurred */
                snackbarHostState.showSnackbar(geoRecordAdErrorMsg)

            GeoRecordImportResult.GeoRecordOutOfBounds ->
                /* Tell the user that the tracks is out of bounds */
                snackbarHostState.showSnackbar(geoRecordOutOfBoundsMsg)
        }
    }

    LaunchedEffectWithLifecycle(recordViewModel.geoRecordRecoverEventFlow) {
        /* Tell the user that a track is being recovered */
        snackbarHostState.showSnackbar(geoRecordRecover)
    }

    LaunchedEffectWithLifecycle(recordViewModel.excursionImportEventFlow) { success ->
        if (success) {
            snackbarHostState.showSnackbar(geoRecordAddMsg)
        } else {
            snackbarHostState.showSnackbar(geoRecordAdErrorMsg)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        statViewModel.importRecordings(uriList)
    }

    val onImportFiles = {
        /* Search for all documents available via installed storage providers */
        launcher.launch("*/*")
    }

    Scaffold(
        topBar = {
            RecordTopAppbar(
                isMultiSelectionMode = isMultiSelectionMode,
                onMainMenuClick = onMainMenuClick,
                onMultiSelectionClick = {
                    isMultiSelectionMode = !isMultiSelectionMode
                },
                onImportClick = onImportFiles
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GpxRecordListStateful(
                modifier = Modifier.padding(8.dp),
                statViewModel = statViewModel,
                recordViewModel = recordViewModel,
                isMultiSelectionMode = isMultiSelectionMode,
                onElevationGraphClick = onElevationGraphClick,
                onGoToTrailSearchClick = onGoToTrailSearchClick,
                onImport = onImportFiles
            )
        }
    }
}