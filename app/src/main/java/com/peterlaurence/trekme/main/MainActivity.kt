package com.peterlaurence.trekme.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.main.shortcut.Shortcut
import com.peterlaurence.trekme.util.android.hasLocationPermission
import com.peterlaurence.trekme.util.collectWhileStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var mapArchiveEvents: MapArchiveEvents

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var appEventBus: AppEventBus

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrekMeTheme {
                MainStateful(
                    viewModel = viewModel,
                    recordingEventHandlerViewModel = hiltViewModel(),
                    appEventBus = appEventBus,
                    gpsProEvents = gpsProEvents,
                    mapArchiveEvents = mapArchiveEvents
                )
            }
        }

        // TODO
//        val versionTextView = headerView.findViewById<TextView>(R.id.app_version)
//        try {
//            val version = "v." + BuildConfig.VERSION_NAME
//            versionTextView.text = version
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }

        appEventBus.billingFlow.collectWhileStarted(this) {
            it.billingClient.launchBillingFlow(this, it.flowParams)
        }
    }

    /**
     * When the activity reaches this lifecycle, fragments aren't created yet.
     * After checking permissions, we asynchronously prepare minimal context initializations before
     * notifying the view-model that everything is ready.
     */
    public override fun onStart() {
        /* Prefetch location now - useful to reduce wait time */
        if (hasLocationPermission()) {
            lifecycleScope.launch {
                locationSource.locationFlow.first()
            }
        }

        val shortcut = when (intent.extras?.getString("shortcutKey")) {
            "recordings" -> Shortcut.RECORDINGS
            "last-map" -> Shortcut.LAST_MAP
            else -> null
        }
        viewModel.onActivityStart(shortcut)

        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onActivityResume()
    }
}
