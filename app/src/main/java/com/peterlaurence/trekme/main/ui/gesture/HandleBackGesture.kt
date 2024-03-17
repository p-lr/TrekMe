package com.peterlaurence.trekme.main.ui.gesture

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.util.android.activity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * If the side menu is opened, just close it.
 * If there's no previous destination, display a confirmation snackbar to back once more before
 * killing the app.
 * Otherwise, navigate up.
 */
@Composable
fun HandleBackGesture(
    drawerState: DrawerState,
    scope: CoroutineScope,
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val activity = LocalContext.current.activity
    val confirmExit = stringResource(id = R.string.confirm_exit)
    BackHandler {
        if (drawerState.isOpen) {
            scope.launch {
                drawerState.close()
            }
        } else {
            if (navController.previousBackStackEntry == null) {
                if (snackbarHostState.currentSnackbarData?.visuals?.message == confirmExit) {
                    activity.finish()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            confirmExit,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            } else {
                navController.navigateUp()
            }
        }
    }
}