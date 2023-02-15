package com.peterlaurence.trekme.features.record.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.presentation.ui.theme.backgroundVariant
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import com.peterlaurence.trekme.features.record.presentation.ui.components.ActionsStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.GpxRecordListStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.RecordTopAppbar
import com.peterlaurence.trekme.features.record.presentation.ui.components.StatusStateful
import com.peterlaurence.trekme.features.record.presentation.viewmodel.GpxRecordServiceViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordViewModel
import com.peterlaurence.trekme.features.record.presentation.viewmodel.RecordingStatisticsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    gpxRecordServiceViewModel: GpxRecordServiceViewModel,
    statViewModel: RecordingStatisticsViewModel,
    recordViewModel: RecordViewModel,
    onElevationGraphClick: (RecordingData) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val deletionFailedMsg = stringResource(id = R.string.files_could_not_be_deleted)
    val geoRecordAddMsg = stringResource(id = R.string.track_is_being_added)
    val geoRecordAdErrorMsg = stringResource(id = R.string.track_add_error)
    LaunchedEffect(LocalLifecycleOwner.current) {
        launch {
            statViewModel.recordingDeletionFailureFlow.collect {
                snackbarHostState.showSnackbar(message = deletionFailedMsg)
            }
        }

        launch {
            recordViewModel.geoRecordImportResultFlow.collect { result ->
                when (result) {
                    is GeoRecordImportResult.GeoRecordImportOk ->
                        /* Tell the user that the track will be shortly available in the map */
                        snackbarHostState.showSnackbar(geoRecordAddMsg)

                    is GeoRecordImportResult.GeoRecordImportError ->
                        /* Tell the user that an error occurred */
                        snackbarHostState.showSnackbar(geoRecordAdErrorMsg)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RecordTopAppbar(onMainMenuClick = recordViewModel::onMainMenuClick)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundVariant())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                ActionsStateful(
                    Modifier
                        .weight(1f)
                        .padding(top = 8.dp, start = 8.dp, end = 4.dp, bottom = 4.dp),
                    viewModel = gpxRecordServiceViewModel
                )
                StatusStateful(
                    Modifier
                        .weight(1f)
                        .padding(top = 8.dp, start = 4.dp, end = 8.dp, bottom = 4.dp),
                    viewModel = gpxRecordServiceViewModel
                )
            }

            GpxRecordListStateful(
                modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                statViewModel = statViewModel,
                recordViewModel = recordViewModel,
                onElevationGraphClick = onElevationGraphClick
            )
        }
    }
}