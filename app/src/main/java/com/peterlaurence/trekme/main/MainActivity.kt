package com.peterlaurence.trekme.main

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.BuildConfig
import com.peterlaurence.trekme.NavGraphDirections
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.databinding.ActivityMainBinding
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.main.eventhandler.MapArchiveEventHandler
import com.peterlaurence.trekme.main.eventhandler.MapDownloadEventHandler
import com.peterlaurence.trekme.main.eventhandler.PermissionRequestHandler
import com.peterlaurence.trekme.main.shortcut.Shortcut
import com.peterlaurence.trekme.util.checkInternet
import com.peterlaurence.trekme.util.collectWhileStarted
import com.peterlaurence.trekme.util.collectWhileStartedIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var fragmentManager: FragmentManager? = null
    lateinit var binding: ActivityMainBinding

    private val navController: NavController
        get() = findNavController(R.id.nav_host_fragment)

    @Inject
    lateinit var mapRepository: MapRepository

    @Inject
    lateinit var mapArchiveEvents: MapArchiveEvents

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var appEventBus: AppEventBus

    private var permissionRequestHandler: PermissionRequestHandler? = null

    private val snackBarExit: Snackbar by lazy {
        Snackbar.make(binding.drawerLayout, R.string.confirm_exit, Snackbar.LENGTH_SHORT)
    }

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fragmentManager = this.supportFragmentManager
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedCustom()
            }
        })

        /* Handle application wide map download events */
        MapDownloadEventHandler(this, lifecycle, downloadRepository,
            onDownloadFinished = { uuid ->
                /* Only if the user is still on the WmtsFragment, navigate to the map list */
                if (getString(R.string.map_wmts_label) == navController.currentDestination?.label) {
                    showMapListFragment(uuid)
                }
            }
        )

        /* Handle application wide map-archive related events */
        MapArchiveEventHandler(this, lifecycle, mapArchiveEvents)

        /* Handle permission request events */
        permissionRequestHandler =
            PermissionRequestHandler(this, lifecycle, appEventBus, gpsProEvents)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = binding.drawerLayout
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        val headerView = binding.navView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.app_version)
        try {
            val version = "v." + BuildConfig.VERSION_NAME
            versionTextView.text = version
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        /* React to some events */
        lifecycleScope.launchWhenCreated {
            viewModel.showMapListSignal.collect {
                showMapListFragment()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.showMapViewSignal.collect {
                showMapFragment()
            }
        }

        /* Only display "Gps Pro" menu if it's purchased */
        lifecycleScope.launchWhenCreated {
            viewModel.gpsProPurchased.collect {
                val gpsItem = binding.navView.menu.findItem(R.id.nav_gps_plus)
                gpsItem.isVisible = it
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.showRecordingsFlow.collect {
                showRecordFragment()
            }
        }

        appEventBus.genericMessageEvents.collectWhileStarted(this) {
            when (it) {
                is StandardMessage -> {
                    val duration = if (it.showLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
                    Snackbar.make(binding.navView, it.msg, duration).show()
                }
                is WarningMessage -> showWarningDialog(it.msg, it.title, null)
            }
        }

        appEventBus.billingFlow.collectWhileStarted(this) {
            it.billingClient.launchBillingFlow(this, it.flowParams)
        }

        appEventBus.openDrawerFlow.map { openDrawer() }.collectWhileStartedIn(this)

        appEventBus.navigateFlow.map { dest ->
            when(dest) {
                AppEventBus.NavDestination.Shop -> showShopFragment()
                AppEventBus.NavDestination.MapList -> showMapListFragment()
                AppEventBus.NavDestination.MapCreation -> showMapCreateFragment()
                AppEventBus.NavDestination.ExcursionSearch -> showExcursionSearchFragment()
            }
        }.collectWhileStartedIn(this)

        gpsProEvents.showBtDeviceSettingsFragmentSignal.collectWhileStarted(this) {
            showBtDeviceSettingsFragment()
        }
    }

    /**
     * If the side menu is opened, just close it.
     * If the navigation component reports that there's no previous destination, display
     * a confirmation snackbar to back once more before killing the app.
     * Otherwise, navigate up.
     */
    private fun onBackPressedCustom() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (navController.previousBackStackEntry == null) {
            /* BACK button twice to exit */
            if (snackBarExit.isShown) {
                finish()
            } else {
                snackBarExit.show()
            }
        } else {
            navController.navigateUp()
        }
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

        val shortcut = when(intent.extras?.getString("shortcutKey")) {
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_select_map -> showMapListFragment()
            R.id.nav_create -> showMapCreateFragment()
            R.id.nav_record -> showRecordFragment()
            R.id.nav_hike_search -> showExcursionSearchFragment()
            R.id.nav_gps_plus -> showGpsProFragment()
            R.id.nav_import -> showMapImportFragment()
            R.id.nav_share -> showWifiP2pFragment()
            R.id.nav_settings -> showSettingsFragment()
            R.id.nav_shop -> showShopFragment()
            R.id.nav_about -> navController.navigate(R.id.action_global_aboutFragment)
            else -> {
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openDrawer() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && !drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    private fun showMapFragment() {
        /* Don't show the fragment if no map has been selected yet */
        if (mapRepository.getCurrentMap() == null) {
            return
        }

        navController.navigate(R.id.action_global_mapFragment)
    }

    /**
     * Navigate to the map-list fragment, optionally providing the id of the map the map-list fragment
     * should immediately scroll to.
     */
    private fun showMapListFragment(mapId: UUID? = null) {
        if (getString(R.string.fragment_map_list) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapListFragment().apply {
                if (mapId != null) {
                    val index = viewModel.getMapIndex(mapId)
                    if (index != -1) {
                        scrollToPosition = index
                    }
                }
            }
            val options = navOptions {
                popUpTo(R.id.mapListFragment) {
                    inclusive = true
                }
            }
            navController.navigate(action, options)
        }
    }

    private fun showMapCreateFragment() {
        if (getString(R.string.fragment_map_create) != navController.currentDestination?.label) {
            val options = navOptions {
                popUpTo(R.id.mapCreateFragment) {
                    inclusive = true
                }
            }
            navController.navigate(NavGraphDirections.actionGlobalMapCreateGraph(), options)
            permissionRequestHandler?.requestMapCreationPermission()
            warnIfNotInternet(getString(R.string.no_internet))
        }
    }

    private fun showMapImportFragment() {
        if (getString(R.string.fragment_map_import) != navController.currentDestination?.label) {
            val options = navOptions {
                popUpTo(R.id.mapImportFragment) {
                    inclusive = true
                }
            }
            navController.navigate(NavGraphDirections.actionGlobalMapImportFragment(), options)
        }
    }

    private fun showWifiP2pFragment() {
        navController.navigate(R.id.action_global_wifiP2pFragment)
    }

    private fun showRecordFragment() {
        if (getString(R.string.fragment_recording) != navController.currentDestination?.label) {
            val options = navOptions {
                popUpTo(R.id.recordFragment) {
                    inclusive = true
                }
            }
            navController.navigate(NavGraphDirections.actionGlobalRecordFragment(), options)
        }
    }

    private fun showExcursionSearchFragment() {
        if (getString(R.string.fragment_excursion_search) != navController.currentDestination?.label) {
            val options = navOptions {
                popUpTo(R.id.excursionSearchFragment) {
                    inclusive = true
                }
            }
            navController.navigate(NavGraphDirections.actionGlobalExcursionSearchFragment(), options)
            warnIfNotInternet(getString(R.string.no_internet_excursions))
        }
    }

    private fun showGpsProFragment() {
        navController.popBackStack(R.id.gpsProFragment, true)
        navController.navigate(R.id.gpsProGraph)
    }

    private fun showBtDeviceSettingsFragment() {
        navController.navigate(R.id.action_gpsProFragment_to_btDeviceSettingsFragment)
    }

    private fun showSettingsFragment() {
        if (getString(R.string.fragment_settings) != navController.currentDestination?.label) {
            navController.navigate(R.id.action_global_settingsFragment)
        }
    }

    private fun showShopFragment() {
        if (getString(R.string.fragment_shop) != navController.currentDestination?.label) {
            navController.navigate(R.id.action_global_shopFragment)
        }
    }

    fun showSnackbar(message: String, isLong: Boolean = true) {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        Snackbar.make(
            drawer, message, if (isLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        ).show()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(ACTIONBAR_TITLE, supportActionBar?.title?.toString())
        outState.putBoolean(ACTIONBAR_VISIBLE, supportActionBar?.isShowing ?: false)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getString(ACTIONBAR_TITLE)?.also {
            supportActionBar?.title = it
        }
        if (savedInstanceState.getBoolean(ACTIONBAR_VISIBLE)) {
            supportActionBar?.show()
        } else {
            supportActionBar?.hide()
        }
    }

    private fun warnIfNotInternet(message: String) {
        lifecycleScope.launchWhenCreated {
            if (!checkInternet()) {
                showSnackbar(message)
            }
        }
    }
}

private const val ACTIONBAR_TITLE = "actionBarTitle"
private const val ACTIONBAR_VISIBLE = "actionBarVisible"