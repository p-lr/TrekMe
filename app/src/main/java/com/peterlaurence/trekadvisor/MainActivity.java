package com.peterlaurence.trekadvisor;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.menu.LocationProvider;
import com.peterlaurence.trekadvisor.menu.MapProvider;
import com.peterlaurence.trekadvisor.menu.MarkerProvider;
import com.peterlaurence.trekadvisor.menu.mapcalibration.MapCalibrationFragment;
import com.peterlaurence.trekadvisor.menu.mapimport.MapImportFragment;
import com.peterlaurence.trekadvisor.menu.maplist.MapListFragment;
import com.peterlaurence.trekadvisor.menu.maplist.MapSettingsFragment;
import com.peterlaurence.trekadvisor.menu.mapview.MapViewFragment;
import com.peterlaurence.trekadvisor.menu.mapview.components.markermanage.MarkerManageFragment;
import com.peterlaurence.trekadvisor.menu.tracksmanage.TracksManageFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MapListFragment.OnMapListFragmentInteractionListener,
        MapViewFragment.RequestManageTracksListener,
        MapSettingsFragment.MapCalibrationRequestListener,
        MarkerManageFragment.MarkerManageFragmentInteractionListener,
        MapProvider,
        MarkerProvider,
        TracksManageFragment.TrackChangeListenerProvider,
        MapViewFragment.RequestManageMarkerListener,
        LocationProvider {

    private static final String MAP_FRAGMENT_TAG = "mapFragment";
    private static final String MAP_LIST_FRAGMENT_TAG = "mapListFragment";
    private static final String MAP_SETTINGS_FRAGMENT_TAG = "mapSettingsFragment";
    private static final String MAP_CALIBRATION_FRAGMENT_TAG = "mapCalibrationFragment";
    private static final String MAP_IMPORT_FRAGMENT_TAG = "mapImportFragment";
    private static final String TRACKS_MANAGE_FRAGMENT_TAG = "tracksManageFragment";
    private static final String MARKER_MANAGE_FRAGMENT_TAG = "markerManageFragment";
    private static final List<String> FRAGMENT_TAGS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(MAP_FRAGMENT_TAG);
                add(MAP_LIST_FRAGMENT_TAG);
                add(MAP_SETTINGS_FRAGMENT_TAG);
                add(MAP_CALIBRATION_FRAGMENT_TAG);
                add(MAP_IMPORT_FRAGMENT_TAG);
                add(TRACKS_MANAGE_FRAGMENT_TAG);
                add(MARKER_MANAGE_FRAGMENT_TAG);
            }});
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_LOCATION = 2;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private static final String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final String TAG = "MainActivity";
    private String mBackFragmentTag;
    private FragmentManager fragmentManager;
    private GoogleApiClient mGoogleApiClient;

    /**
     * Checks whether the app has permission to access fine location.
     * If the app does not have the requested permissions then the user will be prompted.
     */
    public static void checkLocationPermissions(Activity activity) {
        // Check whether we have location permission or not
        int permissionLocation = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            // We don't have the required permissions, so we prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_LOCATION,
                    REQUEST_LOCATION
            );
        }
    }

    /**
     * Checks whether the app has permission to write to device storage.
     * If the app does not have the requested permissions then the user will be prompted.
     */
    public static boolean checkStoragePermission(Activity activity) {
        // Check whether we have write permission or not
        int permissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
            // We don't have the required permissions, so we prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* Create the instance of GoogleAPIClient BEFORE fragments are attached */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        super.onCreate(savedInstanceState);

        fragmentManager = this.getFragmentManager();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }
    }

    /**
     * The first time we create a retained fragment, we don't want it to be added to the back stack,
     * because if the user uses the back button, the fragment manager will "forget" it and there is
     * no way to retrieve with {@link FragmentManager#findFragmentByTag(String)}. <br>
     * Even worse, that last method is used to decide whether a retained fragment should be created
     * or not. So a "forgotten" retained fragment can be created several times. <br>
     * Consequently, the first time a retained fragment is created and shown, it is not added to the
     * back stack. In that particular case, to keep the back functionality, a {@code mBackFragmentTag}
     * is used to store the tag of the fragment to go back to.
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (mBackFragmentTag != null) {
            switch (mBackFragmentTag) {
                case MAP_LIST_FRAGMENT_TAG:
                    showMapListFragment();
                    break;
                case MAP_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                case MAP_SETTINGS_FRAGMENT_TAG:
                    Map map = getSettingsMap();
                    if (map != null) {
                        showMapSettingsFragment(map.getName());
                        break;
                    }
                case TRACKS_MANAGE_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                case MARKER_MANAGE_FRAGMENT_TAG:
                    showMapViewFragment();
                    break;
                default:
                    showMapListFragment();
            }

            /* Clear the tag so that the back stack is popped the next time, unless a new retained
             * fragment is created in the meanwhile.
             */
            mBackFragmentTag = null;
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            showMapListFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Show the app title */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }

        /* Add items to the toolbar */
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onStart() {
        /* Start the location service */
        mGoogleApiClient.connect();

        super.onStart();

        /* Show the map list fragment if we have the right to read the sd card */
        if (checkStoragePermission(this)) {
            /* If the list fragment already exists, the activity might have been recreated because
             * of a configuration change. Then we don't want to show this fragment, as another
             * one is probably already visible.
             */
            Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
            if (mapListFragment == null) {
                showMapListFragment();
            }
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_map:
                showMapViewFragment();
                break;

            case R.id.nav_select_map:
                showMapListFragment();
                break;

            case R.id.nav_import:
                showMapImportFragment();
                break;

            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private Fragment createMapViewFragment(FragmentTransaction transaction) {
        Fragment mapFragment = new MapViewFragment();
        transaction.add(R.id.content_frame, mapFragment, MAP_FRAGMENT_TAG);
        return mapFragment;
    }

    private Fragment createMapListFragment(FragmentTransaction transaction) {
        Fragment mapListFragment = MapListFragment.newInstance();
        transaction.add(R.id.content_frame, mapListFragment, MAP_LIST_FRAGMENT_TAG);
        return mapListFragment;
    }

    private Fragment createMapSettingsFragment(FragmentTransaction transaction, String mapName) {
        Fragment mapSettingsFragment = MapSettingsFragment.newInstance(mapName);
        transaction.add(R.id.content_frame, mapSettingsFragment, MAP_SETTINGS_FRAGMENT_TAG);
        return mapSettingsFragment;
    }

    private Fragment createMapCalibrationFragment(FragmentTransaction transaction) {
        Fragment mapCalibrationFragment = new MapCalibrationFragment();
        transaction.add(R.id.content_frame, mapCalibrationFragment, MAP_CALIBRATION_FRAGMENT_TAG);
        return mapCalibrationFragment;
    }

    private Fragment createMapImportFragment(FragmentTransaction transaction) {
        Fragment mapCalibrationFragment = new MapImportFragment();
        transaction.add(R.id.content_frame, mapCalibrationFragment, MAP_IMPORT_FRAGMENT_TAG);
        return mapCalibrationFragment;
    }

    @Override
    public void onRequestManageTracks() {
        showTracksManageFragment();
    }

    @Override
    public void onRequestManageMarker(MarkerGson.Marker marker) {
        showMarkerManageFragment();
    }

    private void showMapViewFragment() {
        /* Don't show the fragment if no map has been selected yet */
        if (getCurrentMap() == null) {
            return;
        }

        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);

        if (mapFragment == null) {
            mapFragment = createMapViewFragment(transaction);
        }
        transaction.show(mapFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
        transaction.commit();
    }

    private void showTracksManageFragment() {
        try {
            showSingleUsageFragment(TRACKS_MANAGE_FRAGMENT_TAG, TracksManageFragment.class);
        } catch (IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Error while creating " + TRACKS_MANAGE_FRAGMENT_TAG);
        }
    }

    private void showMarkerManageFragment() {
        try {
            showSingleUsageFragment(MARKER_MANAGE_FRAGMENT_TAG, MarkerManageFragment.class);
        } catch (IllegalAccessException | InstantiationException e) {
            Log.e(TAG, "Error while creating " + MARKER_MANAGE_FRAGMENT_TAG);
        }
    }

    private <T extends Fragment> void showSingleUsageFragment(String tag, Class<T> fragmentType) throws IllegalAccessException, InstantiationException {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        T fragment = fragmentType.newInstance();

        hideAllFragments();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_frame, fragment, tag);
        transaction.show(fragment);

        /* Manually manage the back action*/
        mBackFragmentTag = tag;
        transaction.commit();
    }

    private void showMapListFragment() {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_LIST_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);

        /* Show the map list fragment if it exists */
        if (mapListFragment == null) {
            mapListFragment = createMapListFragment(transaction);
        }
        transaction.show(mapListFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_FRAGMENT_TAG;
        transaction.commit();
    }

    private void showMapSettingsFragment(String mapName) {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_SETTINGS_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapSettingsFragment = fragmentManager.findFragmentByTag(MAP_SETTINGS_FRAGMENT_TAG);

        /* Show the map settings fragment if it exists */
        if (mapSettingsFragment == null) {
            mapSettingsFragment = createMapSettingsFragment(transaction, mapName);
        } else {
            /* If it already exists, set the Map */
            ((MapSettingsFragment) mapSettingsFragment).setMap(mapName);
        }
        transaction.show(mapSettingsFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_FRAGMENT_TAG;
        transaction.commit();
    }

    private void showMapCalibrationFragment() {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_CALIBRATION_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapCalibrationFragment = createMapCalibrationFragment(transaction);
        transaction.show(mapCalibrationFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_SETTINGS_FRAGMENT_TAG;
        transaction.commit();
    }

    private void showMapImportFragment() {
        /* Remove single-usage fragments */
        removeSingleUsageFragments();

        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_IMPORT_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapImportFragment = createMapImportFragment(transaction);
        transaction.show(mapImportFragment);

        /* Manually manage the back action*/
        mBackFragmentTag = MAP_LIST_FRAGMENT_TAG;
        transaction.commit();
    }

    /**
     * Hides all fragments except the one which tag is {@code fragmentTag}.
     *
     * @param transaction The {@link FragmentTransaction} object
     * @param fragmentTag The tag of the fragment that should not be hidden
     */
    private void hideOtherFragments(FragmentTransaction transaction, String fragmentTag) {
        if (!FRAGMENT_TAGS.contains(fragmentTag)) {
            return;
        }
        for (String tag : FRAGMENT_TAGS) {
            if (tag.equals(fragmentTag)) continue;
            Fragment otherFragment = fragmentManager.findFragmentByTag(tag);
            if (otherFragment != null) {
                transaction.hide(otherFragment);
            }
        }
    }

    private void removeSingleUsageFragments() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        /* Remove the calibration fragment */
        Fragment mapCalibrationFragment = fragmentManager.findFragmentByTag(MAP_CALIBRATION_FRAGMENT_TAG);
        if (mapCalibrationFragment != null) {
            transaction.remove(mapCalibrationFragment);
        }

        /* Remove the fragment for tracks management */
        Fragment tracksManageFragment = fragmentManager.findFragmentByTag(TRACKS_MANAGE_FRAGMENT_TAG);
        if (tracksManageFragment != null) {
            transaction.remove(tracksManageFragment);
        }

        /* Remove the fragment for marker management */
        Fragment markerManageFragment = fragmentManager.findFragmentByTag(MARKER_MANAGE_FRAGMENT_TAG);
        if (markerManageFragment != null) {
            transaction.remove(markerManageFragment);
        }

        /* Remove the map-import fragment */
        Fragment mapImportFragment = fragmentManager.findFragmentByTag(MAP_IMPORT_FRAGMENT_TAG);
        if (mapImportFragment != null) {
            transaction.remove(mapImportFragment);
        }

        transaction.commit();
    }

    /**
     * Hides all fragments which have a TAG.
     */
    private void hideAllFragments() {
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        for (String tag : FRAGMENT_TAGS) {
            Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                hideTransaction.hide(fragment);
            }
        }
        hideTransaction.commit();
    }

    /**
     * A map has been selected from the {@link MapListFragment}. <br>
     * Updates the current map reference and show the {@link MapViewFragment}.
     */
    @Override
    public void onMapSelectedFragmentInteraction(Map map) {
        showMapViewFragment();
    }

    @Override
    public void onMapSettingsFragmentInteraction(Map map) {
        /* The setting button of a map has been clicked */
        showMapSettingsFragment(map.getName());
    }

    @Override
    public void onMapCalibrationRequest() {
        /* A map has been selected from the MapSettingsFragment to be calibrated. */
        showMapCalibrationFragment();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showMapListFragment();

                    /* For instance check the location permission here */
                    checkLocationPermissions(this);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    @Nullable
    public Map getCurrentMap() {
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
        if (mapListFragment != null && mapListFragment instanceof MapListFragment) {
            return ((MapListFragment) mapListFragment).getCurrentMap();
        }
        return null;
    }

    @Override
    @Nullable
    public Map getSettingsMap() {
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
        if (mapListFragment != null && mapListFragment instanceof MapListFragment) {
            return ((MapListFragment) mapListFragment).getSettingsMap();
        }
        return null;
    }

    @Nullable
    @Override
    public MarkerGson.Marker getCurrentMarker() {
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null && mapViewFragment instanceof MapViewFragment) {
            return ((MapViewFragment) mapViewFragment).getCurrentMarker();
        }
        return null;
    }

    @Override
    public void currentMarkerEdited() {
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null && mapViewFragment instanceof MapViewFragment) {
            ((MapViewFragment) mapViewFragment).currentMarkerEdited();
        }
    }

    @Override
    public TracksManageFragment.TrackChangeListener getTrackChangeListener() {
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null) {
            return ((MapViewFragment) mapViewFragment).getTrackChangeListener();
        }
        return null;
    }


    @Override
    public void registerLocationListener(GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                         GoogleApiClient.OnConnectionFailedListener connFailedListener) {
        mGoogleApiClient.registerConnectionCallbacks(connectionCallbacks);
        mGoogleApiClient.registerConnectionFailedListener(connFailedListener);
    }

    @Override
    public void requestLocationUpdates(LocationListener listener, LocationRequest request) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, request, listener);
        }
    }

    @Override
    public void removeLocationListener(LocationListener listener) {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, listener);
    }

    @Override
    public void showCurrentMap() {
        showMapViewFragment();
    }
}
