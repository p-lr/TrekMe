package com.peterlaurence.trekme.features.record.presentation.ui.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.peterlaurence.trekme.features.record.presentation.ui.RecordListStateful
import com.peterlaurence.trekme.features.record.presentation.ui.components.elevationgraph.ElevationStateful
import com.peterlaurence.trekme.features.record.presentation.viewmodel.ElevationViewModel
import com.peterlaurence.trekme.util.android.activity


fun NavGraphBuilder.recordGraph(
    navController: NavController,
    onNavigateToTrailSearch: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    navigation(startDestination = recordListDestination, route = recordGraph) {
        recordListDestination(
            onNavigateToElevationGraph = {
                navController.navigateToElevationGraph(it)
            },
            onNavigateToTrailSearch = onNavigateToTrailSearch,
            onMainMenuClick = onMainMenuClick
        )

        elevationGraphDestination(
            onBack = { navController.popBackStack() }
        )
    }
}

private fun NavGraphBuilder.recordListDestination(
    onNavigateToElevationGraph: (String) -> Unit,
    onNavigateToTrailSearch: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    composable(route = recordListDestination) {
        RecordListStateful(
            statViewModel = hiltViewModel(LocalContext.current.activity),
            recordViewModel = hiltViewModel(LocalContext.current.activity),
            onElevationGraphClick = onNavigateToElevationGraph,
            onGoToTrailSearchClick = onNavigateToTrailSearch,
            onMainMenuClick = onMainMenuClick,
            onRecordClick = {} // TODO
        )
    }
}

private fun NavGraphBuilder.elevationGraphDestination(onBack: () -> Unit) {
    composable(
        route = "$elevationGraphDestination/{$recordingDataId}",
        arguments = listOf(
            navArgument(recordingDataId) { type = NavType.StringType },
        )
    ) {
        val id = it.arguments?.getString(recordingDataId) ?: return@composable
        val viewModel = hiltViewModel<ElevationViewModel>()
        remember(viewModel) {
            viewModel.onUpdateGraph(id)
        }

        ElevationStateful(
            viewModel = viewModel,
            onBack = onBack
        )
    }
}

private fun NavController.navigateToElevationGraph(recordingDataId: String) {
    navigate("$elevationGraphDestination/$recordingDataId")
}

const val recordGraph = "recordGraph"
const val recordListDestination = "recordListDestination"
private const val elevationGraphDestination = "elevationGraphDestination"

private const val recordingDataId = "recordingDataId"