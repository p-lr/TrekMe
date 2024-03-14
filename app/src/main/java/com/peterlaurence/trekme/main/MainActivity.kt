package com.peterlaurence.trekme.main

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.SetMapInteractor
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.main.eventhandler.MapArchiveEventHandler
import com.peterlaurence.trekme.main.eventhandler.MapDownloadEventHandler
import com.peterlaurence.trekme.main.eventhandler.PermissionRequestHandler
import com.peterlaurence.trekme.main.eventhandler.RecordingEventHandler
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
    lateinit var setMapInteractor: SetMapInteractor

    @Inject
    lateinit var mapArchiveEvents: MapArchiveEvents

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var gpxRecordEvents: GpxRecordEvents

    @Inject
    lateinit var mapExcursionInteractor: MapExcursionInteractor

    @Inject
    lateinit var getMapInteractor: GetMapInteractor

    @Inject
    lateinit var appEventBus: AppEventBus

    private var permissionRequestHandler: PermissionRequestHandler? = null

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrekMeTheme {
                MainScreen(
                    viewModel,
                    appEventBus.genericMessageEvents
                )
            }
        }

        /* Handle application wide map download events */
        MapDownloadEventHandler(this, lifecycle, downloadRepository,
            onMapDownloadFinished = { uuid ->
                /* Only if the user is still on the WmtsFragment, navigate to the map list */
                // TODO
//                if (getString(R.string.map_wmts_label) == navController.currentDestination?.label) {
//                    showMapListFragment()
//                }
//                val snackbar = showSnackbar(
//                    getString(R.string.service_download_finished),
//                    isLong = true
//                )
//                snackbar.setAction(getString(R.string.show_map_action)) {
//                    lifecycleScope.launch {
//                        setMapInteractor.setMap(uuid)
//                        // TODO
//                        // showMapFragment()
//                    }
//                }
            },
            onMapUpdateFinished = { uuid, isRepair ->
                // TODO
//                val snackbar = showSnackbar(
//                    message = if (isRepair) {
//                        getString(R.string.service_repair_finished)
//                    } else {
//                        getString(R.string.service_update_finished)
//                    },
//                    isLong = true
//                ) ?: return@MapDownloadEventHandler
//                snackbar.setAction(getString(R.string.show_map_action)) {
//                    lifecycleScope.launch {
//                        setMapInteractor.setMap(uuid)
//                        // TODO
//                        // showMapFragment()
//                    }
//                }
            }
        )

        /* Handle recording events */
        RecordingEventHandler(
            lifecycle,
            gpxRecordEvents,
            mapExcursionInteractor,
            getMapInteractor,
            onImportDone = { importCount ->
                // TODO
                val msg = getString(R.string.automatic_import_feedback, importCount)
//                showSnackbar(msg)
            }
        )

        /* Handle application wide map-archive related events */
        MapArchiveEventHandler(this, lifecycle, appEventBus, mapArchiveEvents)

        /* Handle permission request events */
        permissionRequestHandler =
            PermissionRequestHandler(this, lifecycle, appEventBus, gpsProEvents)

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

    // TODO
//    private fun showSnackbar(message: String, isLong: Boolean = false): Snackbar {
//        val duration = if (isLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
//        return Snackbar.make(binding.navView, message, duration).also {
//            it.show()
//        }
//    }

    /**
     * If the side menu is opened, just close it.
     * If the navigation component reports that there's no previous destination, display
     * a confirmation snackbar to back once more before killing the app.
     * Otherwise, navigate up.
     */
    private fun onBackPressedCustom() {
//        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
//        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
//            drawer.closeDrawer(GravityCompat.START)
//        } else if (navController.previousBackStackEntry == null) {
//            /* BACK button twice to exit */
//            if (snackBarExit.isShown) {
//                finish()
//            } else {
//                snackBarExit.show()
//            }
//        } else {
//            navController.navigateUp()
//        }
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
        permissionRequestHandler?.requestMinimalPermission()

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
