package com.peterlaurence.trekadvisor;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.menu.CurrentMapProvider;
import com.peterlaurence.trekadvisor.menu.mapcalibration.MapCalibrationFragment;
import com.peterlaurence.trekadvisor.menu.mapimport.MapImportFragment;
import com.peterlaurence.trekadvisor.menu.maplist.MapListFragment;
import com.peterlaurence.trekadvisor.menu.maplist.MapSettingsFragment;
import com.peterlaurence.trekadvisor.menu.mapview.MapViewFragment;
import com.peterlaurence.trekadvisor.menu.tracksmanage.TracksManageFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        MapListFragment.OnMapListFragmentInteractionListener,
        MapViewFragment.RequestManageTracksListener,
        MapSettingsFragment.MapCalibrationRequestListener,
        CurrentMapProvider,
        TracksManageFragment.TrackChangeListenerProvider {

    private static final String MAP_FRAGMENT_TAG = "mapFragment";
    private static final String MAP_LIST_FRAGMENT_TAG = "mapListFragment";
    private static final String MAP_SETTINGS_FRAGMENT_TAG = "mapSettingsFragment";
    private static final String MAP_CALIBRATION_FRAGMENT_TAG = "mapCalibrationFragment";
    private static final String MAP_IMPORT_FRAGMENT_TAG = "mapImportFragment";
    private static final String TRACKS_MANAGE_FRAGMENT_TAG = "tracksManageFragment";

    private static final List<String> FRAGMENT_TAGS = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add(MAP_FRAGMENT_TAG);
                add(MAP_LIST_FRAGMENT_TAG);
                add(MAP_SETTINGS_FRAGMENT_TAG);
                add(MAP_CALIBRATION_FRAGMENT_TAG);
                add(MAP_IMPORT_FRAGMENT_TAG);
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

    private FragmentManager fragmentManager;
    private Map mCurrentMap;

    private static final String TAG = "MainActivity";

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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onStart() {
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

    @Override
    public void onRequestManageTracks(Map map) {
        TracksManageFragment tracksManageFragment = new TracksManageFragment();

        hideAllFragments();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_frame, tracksManageFragment, TRACKS_MANAGE_FRAGMENT_TAG);
        transaction.show(tracksManageFragment);

        /* Hide the MapViewFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapViewFragment.
         */
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null) {
            transaction.hide(mapViewFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
        transaction.commit();

//        /* A transaction commit does not happen immediately; it will be scheduled as work on the
//         * main thread to be done the next time that thread is ready. But we need it to be done
//         * right now.
//         */
//        fragmentManager.executePendingTransactions();
//        tracksManageFragment.generateTracks(map);
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
        Fragment mapCalibrationFragment = MapCalibrationFragment.newInstance();
        transaction.add(R.id.content_frame, mapCalibrationFragment, MAP_CALIBRATION_FRAGMENT_TAG);
        return mapCalibrationFragment;
    }

    private Fragment createMapImportFragment(FragmentTransaction transaction) {
        Fragment mapCalibrationFragment = new MapImportFragment();
        transaction.add(R.id.content_frame, mapCalibrationFragment, MAP_IMPORT_FRAGMENT_TAG);
        return mapCalibrationFragment;
    }

    private void showMapViewFragment() {
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

        /* Hide the MapListFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapListFragment.
         */
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
        if (mapListFragment != null) {
            transaction.hide(mapListFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
    }

    private void showMapListFragment() {
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

        /* Hide the MapViewFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapViewFragment.
         */
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null) {
            transaction.hide(mapViewFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
//        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
    }

    private void showMapSettingsFragment(String mapName) {
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

        /* Hide the MapListFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapListFragment.
         */
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
        if (mapListFragment != null) {
            transaction.hide(mapListFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showMapCalibrationFragment() {
        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_CALIBRATION_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapCalibrationFragment = fragmentManager.findFragmentByTag(MAP_CALIBRATION_FRAGMENT_TAG);

        /* Show the map calibration fragment if it exists */
        if (mapCalibrationFragment == null) {
            mapCalibrationFragment = createMapCalibrationFragment(transaction);
        }
        transaction.show(mapCalibrationFragment);

        /* Hide the MapSettingsFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapSettingsFragment.
         */
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_SETTINGS_FRAGMENT_TAG);
        if (mapListFragment != null) {
            transaction.hide(mapListFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showMapImportFragment() {
        /* Hide other fragments */
        FragmentTransaction hideTransaction = fragmentManager.beginTransaction();
        hideOtherFragments(hideTransaction, MAP_IMPORT_FRAGMENT_TAG);
        hideTransaction.commit();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment mapImportFragment = fragmentManager.findFragmentByTag(MAP_IMPORT_FRAGMENT_TAG);

        /* Show the map calibration fragment if it exists */
        if (mapImportFragment == null) {
            mapImportFragment = createMapImportFragment(transaction);
        }
        transaction.show(mapImportFragment);

        /* Hide the MapListFragment. As this transaction is part of the last commit, the next
         * backStack pop will revert this and thus show the MapListFragment.
         */
        Fragment mapListFragment = fragmentManager.findFragmentByTag(MAP_LIST_FRAGMENT_TAG);
        if (mapListFragment != null) {
            transaction.hide(mapListFragment);
        }

        // Add the transaction to the back stack to allow the use of the Back button
        transaction.addToBackStack(null);
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
        mCurrentMap = map;
        showMapViewFragment();
    }

    @Override
    public void onMapSettingsFragmentInteraction(Map map) {
        /* The setting button of a map has been clicked */
        showMapSettingsFragment(map.getName());
    }

    @Override
    public void onMapCalibrationRequest(Map map) {
        /* A map has been selected from the MapSettingsFragment to be calibrated. */
        showMapCalibrationFragment();

        /* Set the map */
        fragmentManager.executePendingTransactions();
        MapCalibrationFragment calibrationFragment = (MapCalibrationFragment) fragmentManager.findFragmentByTag(MAP_CALIBRATION_FRAGMENT_TAG);
        if (calibrationFragment == null) {
            return;
        }
        calibrationFragment.setMap(map);
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
    public Map getCurrentMap() {
        return mCurrentMap;
    }

    @Override
    public TracksManageFragment.TrackChangeListener getTrackChangeListener() {
        Fragment mapViewFragment = fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if (mapViewFragment != null) {
            return (TracksManageFragment.TrackChangeListener) mapViewFragment;
        }
        return null;
    }
}
