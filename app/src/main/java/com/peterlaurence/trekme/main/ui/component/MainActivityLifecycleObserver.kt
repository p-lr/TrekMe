package com.peterlaurence.trekme.main.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.main.shortcut.Shortcut
import com.peterlaurence.trekme.main.viewmodel.MainActivityViewModel
import com.peterlaurence.trekme.util.android.activity
import com.peterlaurence.trekme.util.compose.LifeCycleObserver

@Composable
fun MainActivityLifecycleObserver(
    viewModel: MainActivityViewModel
) {
    val activity = LocalContext.current.activity as MainActivity
    LifeCycleObserver(
        onStart = {
            val shortcut = when (activity.intent.extras?.getString("shortcutKey")) {
                "recordings" -> Shortcut.RECORDINGS
                "last-map" -> Shortcut.LAST_MAP
                else -> null
            }

            viewModel.onActivityStart(shortcut)
        },
        onResume = {
            viewModel.onActivityResume()
        }
    )
}