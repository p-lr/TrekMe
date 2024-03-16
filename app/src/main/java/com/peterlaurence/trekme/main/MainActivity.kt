package com.peterlaurence.trekme.main

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.main.eventhandler.MapArchiveEventHandler
import com.peterlaurence.trekme.main.shortcut.Shortcut
import com.peterlaurence.trekme.util.android.hasLocationPermission
import com.peterlaurence.trekme.util.collectWhileStarted
import com.peterlaurence.trekme.util.collectWhileStartedIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var mapRepository: MapRepository

    @Inject
    lateinit var mapArchiveEvents: MapArchiveEvents

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var getMapInteractor: GetMapInteractor

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
                    appEventBus,
                    gpsProEvents
                )
            }
        }

        /* Handle application wide map-archive related events */
        MapArchiveEventHandler(this, lifecycle, appEventBus, mapArchiveEvents)

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

        // TODO: remove flow
//        appEventBus.openDrawerFlow.map { openDrawer() }.collectWhileStartedIn(this)

        // TODO: remove flow
        appEventBus.navigateFlow.map { dest ->
//            when(dest) {
//                AppEventBus.NavDestination.Shop -> showShopFragment()
//                AppEventBus.NavDestination.MapList -> showMapListFragment()
//                AppEventBus.NavDestination.MapCreation -> showMapCreateFragment()
//                AppEventBus.NavDestination.TrailSearch -> showTrailSearchFragment()
//            }
        }.collectWhileStartedIn(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* Show the app title */
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        return true
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

    fun showWarningDialog(
        message: String,
        title: String,
        onDismiss: DialogInterface.OnDismissListener?
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message).setTitle(title)
        builder.setPositiveButton(getString(R.string.ok_dialog), null)
        if (onDismiss != null) {
            builder.setOnDismissListener(onDismiss)
        }
        val dialog = builder.create()
        dialog.show()
    }
}
