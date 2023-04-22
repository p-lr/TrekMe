package com.peterlaurence.trekme.features.map.presentation.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.modal.BeaconServiceLauncher
import com.peterlaurence.trekme.features.map.presentation.ui.MapStateful
import com.peterlaurence.trekme.util.android.activity
import java.util.*

internal const val mapDestination = "map_dest"

fun NavGraphBuilder.mapScreen(
    onNavigateToTrackManage: () -> Unit,
    onNavigateToMarkerEdit: (markerId: String, mapId: UUID) -> Unit,
    onNavigateToBeaconEdit: (beaconId: String, mapId: UUID) -> Unit
) {
    composable(mapDestination) {
        TrekMeTheme {
            /* By changing the view-model store owner to the activity in the current
             * composition tree (which in this case starts at setContent { .. }) in the
             * fragment, calling viewModel() inside a composable will provide us a
             * view-model scoped to the activity.
             * When this fragment layer will be removed, don't keep that CompositionLocalProvider,
             * since the composition tree will start at the activity - so this won't be needed
             * anymore. */
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides LocalContext.current.activity
            ) {
                MapStateful(
                    onNavigateToTracksManage = onNavigateToTrackManage,
                    onNavigateToMarkerEdit = onNavigateToMarkerEdit,
                    onNavigateToBeaconEdit = onNavigateToBeaconEdit
                )
            }

            BeaconServiceLauncher()
        }
    }
}