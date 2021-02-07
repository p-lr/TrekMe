package com.peterlaurence.trekme

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.events.WarningMessage
import com.peterlaurence.trekme.databinding.ActivityMainBinding
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.service.event.*
import com.peterlaurence.trekme.ui.maplist.events.*
import com.peterlaurence.trekme.util.collectWhileStarted
import com.peterlaurence.trekme.viewmodel.MainActivityViewModel
import com.peterlaurence.trekme.viewmodel.mapsettings.MapSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var fragmentManager: FragmentManager? = null
    private lateinit var binding: ActivityMainBinding

    private val navController: NavController by lazy {
        findNavController(R.id.nav_host_fragment)
    }

    @Inject
    lateinit var mapRepository: MapRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    lateinit var appEventBus: AppEventBus

    private val snackBarExit: Snackbar by lazy {
        Snackbar.make(binding.drawerLayout, R.string.confirm_exit, Snackbar.LENGTH_SHORT)
    }
    private val viewModel: MainActivityViewModel by viewModels()

    /* Used for notifications */
    private var builder: Notification.Builder? = null
    private var notifyMgr: NotificationManager? = null

    companion object {
        /* Permission-group codes */
        private const val REQUEST_LOCATION = 1
        private const val REQUEST_MAP_CREATION = 2
        private const val REQUEST_STORAGE = 3
        private val PERMISSION_STORAGE = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        @RequiresApi(VERSION_CODES.Q)
        private val PERMISSION_BACKGROUND_LOC = arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )

        private val PERMISSION_LOCATION = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
        )

        private val PERMISSIONS_MAP_CREATION = arrayOf(
                Manifest.permission.INTERNET
        )

        /**
         * Checks whether the app has permission to access fine location and (for Android < 10) to
         * write to device storage.
         * If the app does not have the requested permissions then the user will be prompted.
         */
        fun requestMinimalPermissions(activity: Activity) {
            if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
                /* We absolutely need storage perm under Android 10 */
                val permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            activity,
                            PERMISSION_STORAGE,
                            REQUEST_STORAGE
                    )
                }
            }

            /* Always ask for location perm - even for Android 10 */
            val permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSION_LOCATION,
                        REQUEST_LOCATION
                )
            }
        }

        /**
         * Android 10 and up only: request background location permission.
         */
        fun requestBackgroundLocationPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT < 29) return
            val permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        PERMISSION_BACKGROUND_LOC,
                        REQUEST_LOCATION
                )
            }
        }

        private fun shouldInit(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
                val permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                return permissionWrite == PackageManager.PERMISSION_GRANTED
            }
            return true // we don't need write permission for Android >= 10
        }

        private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (context != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }

    private fun checkMapCreationPermission(): Boolean {
        return hasPermissions(this, *PERMISSIONS_MAP_CREATION)
    }

    /**
     * Request all the required permissions for map creation.
     */
    private fun requestMapCreationPermission() {
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS_MAP_CREATION,
                REQUEST_MAP_CREATION
        )
    }

    /**
     * Determine if we have an internet connection.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun checkInternet(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ip = InetAddress.getByName("google.com")
            ip.hostAddress != ""
        } catch (e: Throwable) {
            false
        }
    }

    private fun warnIfNotInternet() {
        lifecycleScope.launchWhenCreated {
            if (!checkInternet()) {
                showMessageInSnackbar(getString(R.string.no_internet))
            }
        }
    }

    private fun warnNoStoragePerm() {
        showWarningDialog(getString(R.string.no_storage_perm), getString(R.string.warning_title)) {
            if (!shouldInit(this)) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapSettingsViewModel = ViewModelProvider(this).get(MapSettingsViewModel::class.java)
        mapSettingsViewModel.zipEvents.observe(this) { e: ZipEvent? ->
            e?.also { event ->
                when (event) {
                    is ZipProgressEvent -> onZipProgressEvent(event)
                    is ZipFinishedEvent -> onZipFinishedEvent(event)
                    ZipError -> {
                        //TODO: Display a warning
                    }
                    is ZipCloseEvent -> {
                        // When resumed, the fragment is notified with this event (this is how LiveData
                        // works). To avoid emitting a new notification for a ZipFinishedEvent, we use
                        // ZipCloseEvent on which we do nothing.
                    }
                }
            }
        }
        fragmentManager = this.supportFragmentManager
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = binding.drawerLayout
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
        }
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        val headerView = binding.navView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.app_version)
        try {
            val version = "v." + packageManager.getPackageInfo(packageName, 0).versionName
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
                showMapViewFragment()
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

        appEventBus.requestBackgroundLocationSignal.collectWhileStarted(this) {
            requestBackgroundLocationPermission(this@MainActivity)
        }

        downloadRepository.downloadEvent.collectWhileStarted(this) {
            onMapDownloadEvent(it)
        }
    }

    /**
     * If the side menu is opened, just close it.
     * Otherwise, if the navigation component reports that there's no previous destination, display
     * a confirmation snackbar to back once more before killing the app.
     */
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (navController.previousBackStackEntry == null) {
            /* BACK button twice to exit */
            if (snackBarExit.isShown) {
                super.onBackPressed()
            } else {
                snackBarExit.show()
            }
        } else super.onBackPressed()
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
        requestMinimalPermissions(this)

        viewModel.onActivityStart()

        super.onStart()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_select_map -> showMapListFragment()
            R.id.nav_create -> showMapCreateFragment()
            R.id.nav_record -> showRecordFragment()
            R.id.nav_import -> showMapImportFragment()
            R.id.nav_share -> showWifiP2pFragment()
            R.id.nav_settings -> showSettingsFragment()
            R.id.nav_about -> navController.navigate(R.id.action_global_aboutFragment)
            else -> {
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showMapViewFragment() {
        /* Don't show the fragment if no map has been selected yet */
        if (mapRepository.getCurrentMap() == null) {
            return
        }

        navController.navigate(R.id.action_global_mapViewFragment)
    }

    /**
     * Navigate to the map-list fragment, optionally providing the id of the map the map-list fragment
     * should immediately scroll to.
     */
    private fun showMapListFragment(mapId: Int? = null) {
        if (getString(R.string.fragment_map_list) != navController.currentDestination?.label) {
            val action = NavGraphDirections.actionGlobalMapListFragment().apply {
                if (mapId != null) {
                    val index = viewModel.getMapIndex(mapId)
                    if (index != -1) {
                        scrollToPosition = index
                    }
                }
            }
            navController.navigate(action)
        }
    }

    private fun showMapCreateFragment() {
        navController.navigate(R.id.action_global_mapCreateFragment)
        if (!checkMapCreationPermission()) {
            requestMapCreationPermission()
        }
        warnIfNotInternet()
    }

    private fun showMapImportFragment() {
        navController.navigate(R.id.action_global_mapImportFragment)
    }

    private fun showWifiP2pFragment() {
        navController.navigate(R.id.action_global_wifiP2pFragment)
    }

    private fun showRecordFragment() {
        navController.navigate(R.id.action_global_recordFragment)
    }

    private fun showSettingsFragment() {
        navController.navigate(R.id.action_global_settingsFragment)
    }

    private fun showMessageInSnackbar(message: String) {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        val snackbar = Snackbar.make(drawer, message, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    private fun showWarningDialog(message: String, title: String, dismiss: DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message).setTitle(title)
        builder.setPositiveButton(getString(R.string.ok_dialog), null)
        if (dismiss != null) {
            builder.setOnDismissListener(dismiss)
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        /* In the case of storage perm, we should restart the app (only applicable for Android < 10) */
        if (requestCode == REQUEST_STORAGE) {
            /* If Android >= 10 we don't need to restart the activity as we don't request write permission */
            if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) return

            if (grantResults.size == 2) {
                /* Storage read perm is at index 0 */
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /* Restart the activity to ensure that every component can access local storage */
                    finish()
                    startActivity(intent)
                } else if (shouldShowRequestPermissionRationale(permissions[0])) {
                    /* User has deny from permission dialog */
                    warnNoStoragePerm()
                } else {
                    /* User has deny permission and checked never show permission dialog so we redirect to Application settings page */
                    Snackbar.make(binding.root, resources.getString(R.string.storage_perm_denied), Snackbar.LENGTH_INDEFINITE)
                            .setAction(resources.getString(R.string.ok_dialog)) {
                                val intent = Intent()
                                intent.action = ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri = Uri.fromParts("package", this@MainActivity.packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                            .show()
                }
            }
        }
    }

    private fun onMapDownloadEvent(event: MapDownloadEvent) {
        when (event) {
            is MapDownloadFinished -> {
                /* Only if the user is still on the GoogleMapWmtsFragment, navigate to the map list */
                if (getString(R.string.google_map_wmts_label) == navController.currentDestination?.label) {
                    showMapListFragment(event.mapId)
                }
                showMessageInSnackbar(getString(R.string.service_download_finished))
            }
            is MapDownloadStorageError -> showWarningDialog(getString(R.string.service_download_bad_storage), getString(R.string.warning_title), null)
            is MapDownloadPending -> {
                // Nothing particular to do, the service which fire those events already sends
                // notifications with the progression.
            }
            is MapDownloadAlreadyRunning -> showWarningDialog(getString(R.string.service_download_already_running), getString(R.string.warning_title), null)
        }
    }

    /**
     * A [Notification] is sent to the user showing the progression in percent. The
     * [NotificationManager] only process one notification at a time, which is handy since
     * it prevents the application from using too much cpu.
     */
    private fun onZipProgressEvent(event: ZipProgressEvent) {
        val notificationChannelId = "trekadvisor_map_save"
        if (builder == null || notifyMgr == null) {
            try {
                notifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            } catch (e: Exception) {
                // notifyMgr will be null
            }
            if (Build.VERSION.SDK_INT >= 26) {
                //This only needs to be run on Devices on Android O and above
                val channel = NotificationChannel(notificationChannelId,
                        getText(R.string.archive_dialog_title), NotificationManager.IMPORTANCE_LOW)
                channel.enableLights(true)
                channel.lightColor = Color.YELLOW
                notifyMgr?.createNotificationChannel(channel)
                builder = Notification.Builder(this, notificationChannelId)
            } else {
                builder = Notification.Builder(this)
            }

            builder?.setSmallIcon(R.drawable.ic_map_black_24dp)
                    ?.setContentTitle(getString(R.string.archive_dialog_title))
            notifyMgr?.notify(event.mapId, builder?.build())
        }
        builder?.setContentText(String.format(getString(R.string.archive_notification_msg), event.mapName))
        builder?.setProgress(100, event.p, false)
        notifyMgr?.notify(event.mapId, builder!!.build())
    }

    private fun onZipFinishedEvent(event: ZipFinishedEvent) {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return

        val archiveOkMsg = getString(R.string.archive_snackbar_finished)
        val builder = builder ?: return
        /* When the loop is finished, updates the notification */
        builder.setContentText(archiveOkMsg) // Removes the progress bar
                .setProgress(0, 0, false)
        notifyMgr?.notify(event.mapId, builder.build())
        Snackbar.make(binding.navView, archiveOkMsg, Snackbar.LENGTH_SHORT).show()
    }
}