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
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.core.TrekMeContext.checkAppDir
import com.peterlaurence.trekme.core.TrekMeContext.init
import com.peterlaurence.trekme.core.TrekMeContext.isAppDirReadOnly
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials.getIGNCredentials
import com.peterlaurence.trekme.model.map.MapModel.getCurrentMap
import com.peterlaurence.trekme.model.map.MapModel.getSettingsMap
import com.peterlaurence.trekme.service.event.MapDownloadEvent
import com.peterlaurence.trekme.service.event.Status
import com.peterlaurence.trekme.ui.LocationProviderHolder
import com.peterlaurence.trekme.ui.events.DrawerClosedEvent
import com.peterlaurence.trekme.ui.mapcalibration.MapCalibrationFragment
import com.peterlaurence.trekme.ui.mapcreate.MapCreateFragment
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSelectedEvent
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSettingsEvent
import com.peterlaurence.trekme.ui.mapcreate.views.GoogleMapWmtsViewFragment.Companion.newInstance
import com.peterlaurence.trekme.ui.mapcreate.views.ign.IgnCredentialsFragment
import com.peterlaurence.trekme.ui.mapimport.MapImportFragment
import com.peterlaurence.trekme.ui.mapimport.MapImportFragment.OnMapArchiveFragmentInteractionListener
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import com.peterlaurence.trekme.ui.maplist.MapListFragment.OnMapListFragmentInteractionListener
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment.MapCalibrationRequestListener
import com.peterlaurence.trekme.ui.maplist.events.ZipCloseEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipFinishedEvent
import com.peterlaurence.trekme.ui.maplist.events.ZipProgressEvent
import com.peterlaurence.trekme.ui.mapview.MapViewFragment
import com.peterlaurence.trekme.ui.mapview.MapViewFragment.RequestManageMarkerListener
import com.peterlaurence.trekme.ui.mapview.MapViewFragment.RequestManageTracksListener
import com.peterlaurence.trekme.ui.mapview.components.markermanage.MarkerManageFragment.Companion.newInstance
import com.peterlaurence.trekme.ui.mapview.components.markermanage.MarkerManageFragment.MarkerManageFragmentInteractionListener
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import com.peterlaurence.trekme.ui.record.RecordFragment
import com.peterlaurence.trekme.ui.settings.SettingsFragment
import com.peterlaurence.trekme.ui.wifip2p.WifiP2pFragment
import com.peterlaurence.trekme.viewmodel.MainActivityViewModel
import com.peterlaurence.trekme.viewmodel.ShowMapListEvent
import com.peterlaurence.trekme.viewmodel.ShowMapViewEvent
import com.peterlaurence.trekme.viewmodel.common.LocationProvider
import com.peterlaurence.trekme.viewmodel.common.LocationProviderFactory
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.EventBusException
import org.greenrobot.eventbus.Subscribe
import java.util.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        OnMapListFragmentInteractionListener, OnMapArchiveFragmentInteractionListener,
        RequestManageTracksListener, MapCalibrationRequestListener,
        MarkerManageFragmentInteractionListener, RequestManageMarkerListener, LocationProviderHolder {
    private var backFragmentTag: String? = null
    private var fragmentManager: FragmentManager? = null
    private var snackBarExit: Snackbar? = null
    private var navigationView: NavigationView? = null
    private var viewModel: MainActivityViewModel? = null
    private var locationProviderFactory: LocationProviderFactory? = null

    /* Used for notifications */
    private var builder: Notification.Builder? = null
    private var notifyMgr: NotificationManager? = null

    companion object {
        private const val MAP_FRAGMENT_TAG = "mapFragment"
        private const val MAP_LIST_FRAGMENT_TAG = "mapListFragment"
        private const val MAP_SETTINGS_FRAGMENT_TAG = "mapSettingsFragment"
        private const val MAP_CALIBRATION_FRAGMENT_TAG = "mapCalibrationFragment"
        private const val MAP_IMPORT_FRAGMENT_TAG = "mapImportFragment"
        private const val MAP_CREATE_FRAGMENT_TAG = "mapCreateFragment"
        private const val IGN_CREDENTIALS_FRAGMENT_TAG = "ignCredentialsFragment"
        private const val WMTS_VIEW_FRAGMENT_TAG = "wmtsViewFragment"
        private const val RECORD_FRAGMENT_TAG = "gpxFragment"
        private const val TRACK_VIEW_FRAGMENT_TAG = "trackViewFragment"
        private const val SETTINGS_FRAGMENT = "settingsFragment"
        private const val TRACKS_MANAGE_FRAGMENT_TAG = "tracksManageFragment"
        private const val MARKER_MANAGE_FRAGMENT_TAG = "markerManageFragment"
        private const val WIFI_P2P_FRAGMENT_TAG = "wifiP2pFragment"
        private val FRAGMENT_TAGS = Collections.unmodifiableList(
                object : ArrayList<String?>() {
                    init {
                        add(MAP_FRAGMENT_TAG)
                        add(MAP_LIST_FRAGMENT_TAG)
                        add(MAP_SETTINGS_FRAGMENT_TAG)
                        add(MAP_CALIBRATION_FRAGMENT_TAG)
                        add(MAP_IMPORT_FRAGMENT_TAG)
                        add(MAP_CREATE_FRAGMENT_TAG)
                        add(IGN_CREDENTIALS_FRAGMENT_TAG)
                        add(WMTS_VIEW_FRAGMENT_TAG)
                        add(TRACKS_MANAGE_FRAGMENT_TAG)
                        add(MARKER_MANAGE_FRAGMENT_TAG)
                        add(RECORD_FRAGMENT_TAG)
                        add(TRACK_VIEW_FRAGMENT_TAG)
                        add(SETTINGS_FRAGMENT)
                        add(WIFI_P2P_FRAGMENT_TAG)
                    }
                })

        /* Permission-group codes */
        private const val REQUEST_MINIMAL = 1
        private const val REQUEST_MAP_CREATION = 2
        private val PERMISSIONS_BELOW_ANDROID_10 = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private val PERMISSIONS_ANDROID_10_AND_ABOVE = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
        )
        private val PERMISSIONS_MAP_CREATION = arrayOf(
                Manifest.permission.INTERNET
        )
        private const val TAG = "MainActivity"
        private const val KEY_BUNDLE_BACK = "keyBackFragmentTag"

        /**
         * Checks whether the app has permission to access fine location and to write to device storage.
         * If the app does not have the requested permissions then the user will be prompted.
         */
        fun requestMinimalPermissions(activity: Activity?) {
            val permissionLocation = ActivityCompat.checkSelfPermission(activity!!, Manifest.permission.ACCESS_FINE_LOCATION)
            val permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (permissionLocation != PackageManager.PERMISSION_GRANTED || permissionWrite != PackageManager.PERMISSION_GRANTED) {
                // We don't have the required permissions, so we prompt the user
                if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
                    ActivityCompat.requestPermissions(
                            activity,
                            PERMISSIONS_BELOW_ANDROID_10,
                            REQUEST_MINIMAL
                    )
                } else {
                    ActivityCompat.requestPermissions(
                            activity,
                            PERMISSIONS_ANDROID_10_AND_ABOVE,
                            REQUEST_MINIMAL
                    )
                }
            }
        }

        private fun checkStoragePermissions(activity: Activity): Boolean {
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

        init {
            /* Setup default eventbus to use an index instead of reflection, which is recommended for
         * Android for best performance.
         * See http://greenrobot.org/eventbus/documentation/subscriber-index
         */
            try {
                EventBus.builder().addIndex(MyEventBusIndex()).installDefaultEventBus()
            } catch (e: EventBusException) {
                // don't care
            }
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
    private fun checkInternet(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnected
        }
        return false
    }

    private fun warnIfNotInternet() {
        if (!checkInternet()) {
            showMessageInSnackbar(getString(R.string.no_internet))
        }
    }

    private fun warnIfBadStorageState() {
        /* If something is wrong.. */
        if (!checkAppDir()) {
            val warningTitle = getString(R.string.warning_title)
            if (isAppDirReadOnly) {
                /* If its read only for sure, be explicit */
                showWarningDialog(getString(R.string.storage_read_only), warningTitle, null)
            } else {
                /* Else, just say there is something wrong */
                showWarningDialog(getString(R.string.bad_storage_status), warningTitle, null)
            }
        }
    }

    private fun warnNoStoragePerm() {
        showWarningDialog(getString(R.string.no_storage_perm), getString(R.string.warning_title),
                DialogInterface.OnDismissListener {
                    if (!checkStoragePermissions(this)) {
                        finish()
                    }
                })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
        val mapListViewModel = ViewModelProvider(this).get(MapListViewModel::class.java)
        mapListViewModel.zipEvents.observe(this, Observer { event: ZipEvent? ->
            when (event) {
                is ZipProgressEvent -> onZipProgressEvent(event)
                is ZipFinishedEvent -> onZipFinishedEvent(event)
                is ZipCloseEvent -> {
                    // When resumed, the fragment is notified with this event (this is how LiveData
                    // works). To avoid emitting a new notification for a ZipFinishedEvent, we use
                    // ZipCloseEvent on which we do nothing.
                }
            }
        })
        fragmentManager = this.supportFragmentManager
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                EventBus.getDefault().post(DrawerClosedEvent())
            }
        }
        drawer?.addDrawerListener(toggle)
        toggle.syncState()
        navigationView = findViewById(R.id.nav_view)
        if (navigationView != null) {
            navigationView!!.setNavigationItemSelectedListener(this)
            val headerView = navigationView!!.getHeaderView(0)
            val versionTextView = headerView.findViewById<TextView>(R.id.app_version)
            try {
                val version = "v." + packageManager.getPackageInfo(packageName, 0).versionName
                versionTextView.text = version
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
        if (drawer != null) {
            snackBarExit = Snackbar.make(drawer, R.string.confirm_exit, Snackbar.LENGTH_SHORT)
        }
    }

    /**
     * The first time we create a retained fragment, we don't want it to be added to the back stack,
     * because if the user uses the back button, the fragment manager will "forget" it and there is
     * no way to retrieve with `FragmentManager.findFragmentByTag(string)`. <br></br>
     * Even worse, that last method is used to decide whether a retained fragment should be created
     * or not. So a "forgotten" retained fragment can be created several times. <br></br>
     * Consequently, the first time a retained fragment is created and shown, it is not added to the
     * back stack. In that particular case, to keep the back functionality, a `mBackFragmentTag`
     * is used to store the tag of the fragment to go back to.
     */
    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else if (backFragmentTag != null) {
            when (backFragmentTag) {
                MAP_LIST_FRAGMENT_TAG -> {
                    showMapListFragment()
                    /* Clear the tag so that the back stack is popped the next time, unless a new retained
                     * fragment is created in the meanwhile. */backFragmentTag = null
                }
                MAP_FRAGMENT_TAG -> showMapViewFragment()
                MAP_SETTINGS_FRAGMENT_TAG -> {
                    val map = getSettingsMap()
                    if (map != null) {
                        showMapSettingsFragment(map.id)
                    } else {
                        backFragmentTag = null
                    }
                    showMapViewFragment()
                }
                TRACKS_MANAGE_FRAGMENT_TAG -> showMapViewFragment()
                MARKER_MANAGE_FRAGMENT_TAG -> showMapViewFragment()
                IGN_CREDENTIALS_FRAGMENT_TAG -> {
                    showMapCreateFragment()
                    backFragmentTag = MAP_LIST_FRAGMENT_TAG
                }
                else -> {
                    showMapListFragment()
                    backFragmentTag = null
                }
            }
        } else if (fragmentManager!!.backStackEntryCount > 0) {
            fragmentManager!!.popBackStack()
        } else {
            /* BACK button twice to exit */
            val snackBarExit = snackBarExit
            if (snackBarExit == null || snackBarExit.isShown) {
                super.onBackPressed()
            } else {
                snackBarExit.show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        /* Show the app title */
        val actionBar = supportActionBar
        actionBar?.setDisplayShowTitleEnabled(true)
        return true
    }

    public override fun onStart() {
        requestMinimalPermissions(this)

        /* This must be done before activity's onStart */
        if (checkStoragePermissions(this)) {
            init(this)
        }
        super.onStart()

        /* Register event-bus */
        EventBus.getDefault().register(this)

        /* Start the view-model */
        viewModel?.onActivityStart()
    }

    @Subscribe
    fun onShowMapListEvent(event: ShowMapListEvent?) {
        showMapListFragment()
        warnIfBadStorageState()
        backFragmentTag = null
    }

    @Subscribe
    fun onShowMapViewEvent(event: ShowMapViewEvent?) {
        showMapViewFragment()
        backFragmentTag = MAP_LIST_FRAGMENT_TAG
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_map -> showMapViewFragment()
            R.id.nav_select_map -> showMapListFragment()
            R.id.nav_create -> showMapCreateFragment()
            R.id.nav_record -> showRecordFragment()
            R.id.nav_import -> showMapImportFragment()
            R.id.nav_share -> showWifiP2pFragment()
            R.id.nav_settings -> showSettingsFragment()
            R.id.nav_help -> {
                val url = getString(R.string.help_url)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
            else -> {
            }
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer?.closeDrawer(GravityCompat.START)
        return true
    }

    private fun createMapViewFragment(transaction: FragmentTransaction): Fragment {
        val mapFragment: Fragment = MapViewFragment()
        transaction.add(R.id.content_frame, mapFragment, MAP_FRAGMENT_TAG)
        return mapFragment
    }

    private fun createMapSettingsFragment(transaction: FragmentTransaction, mapId: Int): Fragment {
        val mapSettingsFragment: Fragment = MapSettingsFragment.newInstance(mapId)
        transaction.add(R.id.content_frame, mapSettingsFragment, MAP_SETTINGS_FRAGMENT_TAG)
        return mapSettingsFragment
    }

    /**
     * Generic strategy to create a [Fragment].
     */
    @Throws(InstantiationException::class, IllegalAccessException::class)
    private fun <T : Fragment> createFragment(transaction: FragmentTransaction, tag: String, fragmentType: Class<T>): Fragment {
        val fragment: Fragment = fragmentType.newInstance()
        transaction.add(R.id.content_frame, fragment, tag)
        return fragment
    }

    private fun createWmtsViewFragment(transaction: FragmentTransaction, mapSource: MapSource): Fragment {
        val wmtsViewFragment: Fragment = newInstance(MapSourceBundle(mapSource))
        transaction.add(R.id.content_frame, wmtsViewFragment, WMTS_VIEW_FRAGMENT_TAG)
        return wmtsViewFragment
    }

    override fun onRequestManageTracks() {
        showTracksManageFragment()
    }

    override fun onRequestManageMarker(mapId: Int, marker: MarkerGson.Marker) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments()
        val fragment = newInstance(mapId, marker)
        hideAllFragments()

        val fragmentManager = fragmentManager ?: return
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.content_frame, fragment, MARKER_MANAGE_FRAGMENT_TAG)
        transaction.show(fragment)

        /* Manually manage the back action*/
        backFragmentTag = MARKER_MANAGE_FRAGMENT_TAG
        transaction.commit()
    }

    val tracksManageFragment: TracksManageFragment?
        get() {
            val fragment = fragmentManager?.findFragmentByTag(TRACKS_MANAGE_FRAGMENT_TAG)
            return if (fragment != null) fragment as TracksManageFragment? else null
        }

    private fun showMapViewFragment() {
        /* Don't show the fragment if no map has been selected yet */
        if (getCurrentMap() == null) {
            return
        }

        /* Remove single-usage fragments */removeSingleUsageFragments()

        /* Hide other fragments */
        val fragmentManager = fragmentManager ?: return
        val hideTransaction = fragmentManager.beginTransaction()
        hideOtherFragments(hideTransaction, MAP_FRAGMENT_TAG)
        hideTransaction.commit()
        val transaction = fragmentManager.beginTransaction()
        var mapFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG)
        if (mapFragment == null) {
            mapFragment = createMapViewFragment(transaction)
        }
        transaction.show(mapFragment)

        /* Manually manage the back action*/
        backFragmentTag = MAP_LIST_FRAGMENT_TAG
        transaction.commit()
    }

    private fun showTracksManageFragment() {
        try {
            showSingleUsageFragment(TRACKS_MANAGE_FRAGMENT_TAG, TracksManageFragment::class.java)
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Error while creating $TRACKS_MANAGE_FRAGMENT_TAG")
        } catch (e: InstantiationException) {
            Log.e(TAG, "Error while creating $TRACKS_MANAGE_FRAGMENT_TAG")
        }
    }

    @Throws(IllegalAccessException::class, InstantiationException::class)
    private fun <T : Fragment> showSingleUsageFragment(tag: String, fragmentType: Class<T>) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments()
        val fragment = fragmentType.newInstance()
        hideAllFragments()
        val transaction = fragmentManager!!.beginTransaction()
        transaction.add(R.id.content_frame, fragment, tag)
        transaction.show(fragment)

        /* Manually manage the back action*/
        backFragmentTag = tag
        transaction.commit()
    }

    private fun showMapListFragment() {
        showFragment(MAP_LIST_FRAGMENT_TAG, null, MapListFragment::class.java)
    }

    private fun showMapSettingsFragment(mapId: Int) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments()

        /* Hide other fragments */
        val hideTransaction = fragmentManager!!.beginTransaction()
        hideOtherFragments(hideTransaction, MAP_SETTINGS_FRAGMENT_TAG)
        hideTransaction.commit()
        val transaction = fragmentManager!!.beginTransaction()
        var mapSettingsFragment = fragmentManager!!.findFragmentByTag(MAP_SETTINGS_FRAGMENT_TAG)

        /* Show the map settings fragment if it exists */if (mapSettingsFragment == null) {
            mapSettingsFragment = createMapSettingsFragment(transaction, mapId)
        } else {
            /* If it already exists, set the Map */
            (mapSettingsFragment as MapSettingsFragment).setMap(mapId)
        }
        transaction.show(mapSettingsFragment)

        /* Manually manage the back action*/
        backFragmentTag = MAP_LIST_FRAGMENT_TAG
        transaction.commit()
    }

    /**
     * Generic way of showing a fragment.
     *
     * @param tag             the tag of the [Fragment] to show
     * @param backFragmentTag the tag of the [Fragment] the back press should lead to
     * @param fragmentType    the class of the [Fragment] to instantiate (if needed)
     */
    private fun <T : Fragment> showFragment(tag: String, backFragmentTag: String?, fragmentType: Class<T>) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments()

        /* Hide other fragments */
        val hideTransaction = fragmentManager!!.beginTransaction()
        hideOtherFragments(hideTransaction, tag)
        hideTransaction.commit()
        val transaction = fragmentManager!!.beginTransaction()
        val fragment: Fragment
        try {
            fragment = createFragment(transaction, tag, fragmentType)
            transaction.show(fragment)

            /* Manually manage the back action */
            this.backFragmentTag = backFragmentTag
            transaction.commit()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun showMapCalibrationFragment() {
        showFragment(MAP_CALIBRATION_FRAGMENT_TAG, MAP_SETTINGS_FRAGMENT_TAG, MapCalibrationFragment::class.java)
    }

    private fun showMapCreateFragment() {
        showFragment(MAP_CREATE_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, MapCreateFragment::class.java)
        if (!checkMapCreationPermission()) {
            requestMapCreationPermission()
        }
        warnIfNotInternet()
    }

    private fun showIgnCredentialsFragment() {
        showFragment(IGN_CREDENTIALS_FRAGMENT_TAG, IGN_CREDENTIALS_FRAGMENT_TAG, IgnCredentialsFragment::class.java)
        warnIfNotInternet()
    }

    private fun showWmtsViewFragment(mapSource: MapSource) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments()

        /* Hide other fragments */
        val hideTransaction = fragmentManager!!.beginTransaction()
        hideOtherFragments(hideTransaction, WMTS_VIEW_FRAGMENT_TAG)
        hideTransaction.commit()
        val transaction = fragmentManager!!.beginTransaction()
        val wmtsViewFragment = createWmtsViewFragment(transaction, mapSource)
        transaction.show(wmtsViewFragment)

        /* Manually manage the back action*/backFragmentTag = WMTS_VIEW_FRAGMENT_TAG
        transaction.commit()
        warnIfNotInternet()
    }

    private fun showMapImportFragment() {
        showFragment(MAP_IMPORT_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, MapImportFragment::class.java)
    }

    private fun showWifiP2pFragment() {
        showFragment(WIFI_P2P_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, WifiP2pFragment::class.java)
    }

    private fun showRecordFragment() {
        showFragment(RECORD_FRAGMENT_TAG, MAP_LIST_FRAGMENT_TAG, RecordFragment::class.java)
    }

    private fun showSettingsFragment() {
        showFragment(SETTINGS_FRAGMENT, MAP_LIST_FRAGMENT_TAG, SettingsFragment::class.java)
    }

    /**
     * Hides all fragments except the one which tag is `fragmentTag`.
     * Oddly, with the exception of fragments extending [androidx.preference.PreferenceFragmentCompat],
     * or it would cause an exception (hard to explain).
     *
     * @param transaction The [FragmentTransaction] object
     * @param fragmentTag The tag of the fragment that should not be hidden
     */
    private fun hideOtherFragments(transaction: FragmentTransaction, fragmentTag: String) {
        if (!FRAGMENT_TAGS.contains(fragmentTag)) {
            return
        }
        for (tag in FRAGMENT_TAGS) {
            if (tag == fragmentTag) continue
            val otherFragment = fragmentManager!!.findFragmentByTag(tag)
            if (otherFragment != null && otherFragment !is PreferenceFragmentCompat) {
                transaction.hide(otherFragment)
            }
        }
    }

    /**
     * Only remove fragments that are NOT retained.
     */
    private fun removeSingleUsageFragments() {
        val transaction = fragmentManager!!.beginTransaction()
        for (tag in FRAGMENT_TAGS) {
            val fragment = fragmentManager!!.findFragmentByTag(tag)
            if (fragment != null && !fragment.retainInstance) {
                transaction.remove(fragment)
            }
        }
        transaction.commit()
    }

    /**
     * Hides all fragments which have a TAG.
     */
    private fun hideAllFragments() {
        val hideTransaction = fragmentManager!!.beginTransaction()
        for (tag in FRAGMENT_TAGS) {
            val fragment = fragmentManager!!.findFragmentByTag(tag)
            if (fragment != null) {
                hideTransaction.hide(fragment)
            }
        }
        hideTransaction.commit()
    }

    private fun showMessageInSnackbar(message: String) {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout) ?: return
        val snackbar = Snackbar.make(drawer, message, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    private fun showWarningDialog(message: String, title: String, dismiss: DialogInterface.OnDismissListener?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message).setTitle(title)
        if (dismiss != null) {
            builder.setOnDismissListener(dismiss)
        }
        val dialog = builder.create()
        dialog.show()
    }

    /**
     * A map has been selected from the [MapListFragment]. <br></br>
     * Updates the current map reference and show the [MapViewFragment].
     */
    override fun onMapSelectedFragmentInteraction(map: Map) {
        showMapViewFragment()
    }

    override fun onMapSettingsFragmentInteraction(map: Map) {
        /* The setting button of a map has been clicked */
        showMapSettingsFragment(map.id)
    }

    override fun onGoToMapCreation() {
        showMapCreateFragment()
    }

    override fun onMapCalibrationRequest() {
        /* A map has been selected from the MapSettingsFragment to be calibrated. */
        showMapCalibrationFragment()
    }

    override fun onMapArchiveFragmentInteraction() {
        showMapListFragment()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        /* If Android >= 10 we don't need to restart the activity as we don't request write permission */
        if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) return

        /* Else, we want to restart the activity */
        if (requestCode == REQUEST_MINIMAL) {
            if (grantResults.size >= 2) {
                /* Storage read perm is at index 1 */
                if (grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    warnNoStoragePerm()
                } else {
                    /* Restart the activity to ensure that every component can access local storage */
                    finish()
                    startActivity(intent)
                }
            }
        }
    }

    override fun showCurrentMap() {
        showMapViewFragment()
    }

    @Subscribe
    fun onMapSourceSelected(event: MapSourceSelectedEvent) {
        when (val mapSource = event.mapSource) {
            MapSource.IGN -> {
                /* Check whether credentials are already set or not */
                val ignCredentials = getIGNCredentials()
                if (ignCredentials == null) {
                    showIgnCredentialsFragment()
                } else {
                    showWmtsViewFragment(mapSource)
                }
            }
            else -> showWmtsViewFragment(mapSource)
        }
    }

    @Subscribe
    fun onMapSourceSettings(event: MapSourceSettingsEvent) {
        when (event.mapSource) {
            MapSource.IGN -> showIgnCredentialsFragment()
            MapSource.OPEN_STREET_MAP -> {
            }
            else -> {
            }
        }
    }

    @Subscribe
    fun onMapDownloadEvent(event: MapDownloadEvent) {
        when (event.status) {
            Status.FINISHED -> showMessageInSnackbar(getString(R.string.service_download_finished))
            Status.STORAGE_ERROR -> showWarningDialog(getString(R.string.service_download_bad_storage), getString(R.string.warning_title), null)
            Status.PENDING -> {
                // Nothing particular to do, the service which fire those events already sends
                // notifications with the progression.
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_BUNDLE_BACK, backFragmentTag)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        backFragmentTag = savedInstanceState.getString(KEY_BUNDLE_BACK)
    }

    override val locationProvider: LocationProvider
        get() {
            if (locationProviderFactory == null) {
                locationProviderFactory = LocationProviderFactory(applicationContext)
            }
            return locationProviderFactory!!.getLocationProvider()
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
                val mChannel = NotificationChannel(notificationChannelId,
                        getText(R.string.archive_dialog_title), NotificationManager.IMPORTANCE_LOW)
                mChannel.enableLights(true)
                mChannel.lightColor = Color.YELLOW
                if (notifyMgr != null) {
                    notifyMgr!!.createNotificationChannel(mChannel)
                }
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
        val archiveOkMsg = getString(R.string.archive_snackbar_finished)
        val builder = builder ?: return
        /* When the loop is finished, updates the notification */
        builder.setContentText(archiveOkMsg) // Removes the progress bar
                .setProgress(0, 0, false)
        notifyMgr?.notify(event.mapId, builder.build())
        if (navigationView != null) {
            val snackbar = Snackbar.make(navigationView!!, archiveOkMsg, Snackbar.LENGTH_SHORT)
            snackbar.show()
        }
    }
}