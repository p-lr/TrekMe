package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.AwaitingLocation
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.Error
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.ExcursionMapViewModel
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.MapReady
import com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.UiState
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun ExcursionMapStateful(
    viewModel: ExcursionMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiStateFlow.collectAsStateWithLifecycle()
    LaunchedEffectWithLifecycle(flow = viewModel.locationFlow) {

    }

    ExcursionMapScreen(uiState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcursionMapScreen(uiState: UiState) {
    Scaffold { paddingValues ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        when (uiState) {
            Error.PROVIDER_OUTAGE -> {
                // TODO
            }

            Loading -> {
                // TODO()
            }

            AwaitingLocation -> {
                // TODO()
            }

            is MapReady -> ExcursionMap(modifier, uiState.mapState)
        }
    }
}

@Composable
private fun ExcursionMap(modifier: Modifier, mapState: MapState) {
    MapUI(modifier = modifier, state = mapState)
}