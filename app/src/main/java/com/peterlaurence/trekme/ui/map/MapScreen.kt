package com.peterlaurence.trekme.ui.map

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.viewmodel.map.Error
import com.peterlaurence.trekme.viewmodel.map.Loading
import com.peterlaurence.trekme.viewmodel.map.MapUiState
import com.peterlaurence.trekme.viewmodel.map.MapViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    LaunchedEffect(lifecycleOwner) {
//        launch {
//            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
//                viewModel.locationFlow.collect {
//                    viewModel.onLocation(it)
//                }
//            }
//        }
    }

    val locationFlowLifecycleAware = remember(viewModel.locationFlow, lifecycleOwner) {
        viewModel.locationFlow.flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
    }

    val location by locationFlowLifecycleAware.collectAsState(null)
    Text("Hello world $location")

    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        Error.LicenseError -> Text(text = "license error")
        Error.EmptyMap -> Text(text = "empty map")
        Loading -> Text(text = "loading")
        is MapUiState -> MapUi(uiState as MapUiState)
    }
}

@Composable
fun MapUi(mapUiState: MapUiState) {
    MapUI(state = mapUiState.mapState)
}
