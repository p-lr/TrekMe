package com.peterlaurence.trekadvisor.menu.mapview;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.projection.Projection;
import com.peterlaurence.trekadvisor.core.projection.ProjectionTask;
import com.peterlaurence.trekadvisor.core.sensors.OrientationSensor;
import com.peterlaurence.trekadvisor.menu.CurrentMapProvider;
import com.peterlaurence.trekadvisor.menu.mapview.components.PathView;
import com.peterlaurence.trekadvisor.menu.tracksmanage.TracksManageFragment;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.widgets.ZoomPanLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A {@link Fragment} subclass that implements required interfaces to be used with a
 * {@link GoogleApiClient}.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link RequestManageTracksListener} and {@link CurrentMapProvider} interfaces to handle
 * interaction events.
 * </p>
 *
 * @author peterLaurence
 */
public class MapViewFragment extends Fragment implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        ProjectionTask.ProjectionUpdateLister,
        FrameLayoutMapView.PositionTouchListener,
        FrameLayoutMapView.LockViewListener,
        TracksManageFragment.TrackChangeListener {

    public static final String TAG = "MapViewFragment";
    static final String MAP_KEY = "MAP_KEY";
    private FrameLayoutMapView rootView;
    private TileViewExtended mTileView;
    private Map mMap;
    private View mPositionMarker;
    private boolean mLockView = false;
    private RequestManageTracksListener mRequestManageTracksListener;
    private CurrentMapProvider mCurrentMapProvider;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private OrientationSensor mOrientationSensor;
    private MarkerLayer mMarkerLayer;

    public MapViewFragment() {
    }

    /**
     * A track file has been parsed. At this stage, the new {@link MapGson.Route} are added to the
     * {@link Map}.
     *
     * @param map       the {@link Map} associated with the change
     * @param routeList a list of {@link MapGson.Route}
     */
    @Override
    public void onTrackChanged(Map map, List<MapGson.Route> routeList) {
        Log.d(TAG, routeList.size() + " new route received for map " + map.getName());

        DrawRoutesTask drawRoutesTask = new DrawRoutesTask(map, routeList, mTileView);
        drawRoutesTask.execute();
    }

    @Override
    public void onTrackVisibilityChanged() {
        mTileView.getPathView().invalidate();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        /* Create the instance of GoogleAPIClient */
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this.getContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        /* Create the instance of the OrientationSensor */
        mOrientationSensor = new OrientationSensor(getContext());

        /* Create the marker layer */
        mMarkerLayer = new MarkerLayer(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Create layout from scratch if it does not exist*/
        if (rootView == null) {
            rootView = new FrameLayoutMapView(this.getContext());
            rootView.setPositionTouchListener(this);
            rootView.setLockViewListener(this);
        } else {
            /* Don't re-create the TileView, it handles configuration changes itself */
            return rootView;
        }

        /**
         * This code bellow should not be reachable in a retained fragment, but in case of design
         * change, this shows that we are able to restore a {@link TileView} state from a former
         * instance of {@link TileView}.
         */
        if (savedInstanceState != null) {
            Parcelable parcelable = savedInstanceState.getParcelable(MAP_KEY);
            if (parcelable != null) {
                try {
                    mMap = (Map) parcelable;
                    setMap(mMap);
                } catch (Exception e) {
                    // no-op
                }
            }
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /* Hide the app title */
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        /* Clear the existing action menu */
        menu.clear();

        /* .. and fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_view, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_marker_id:
                mMarkerLayer.addMarker();
                return true;
            case R.id.manage_tracks_id:
                mRequestManageTracksListener.onRequestManageTracks(mMap);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPositionTouch() {
        centerOnPosition();
    }

    @Override
    public void onLockView(boolean lock) {
        mLockView = lock;
    }

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();

        updateMapIfNecessary();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            updateMapIfNecessary();
        }
    }

    /**
     * Only update the map if its a new one.
     */
    private void updateMapIfNecessary() {
        Map map = mCurrentMapProvider.getCurrentMap();
        if (mMap == null || mMap != map) {
            setMap(map);
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RequestManageTracksListener && context instanceof CurrentMapProvider) {
            mRequestManageTracksListener = (RequestManageTracksListener) context;
            mCurrentMapProvider = (CurrentMapProvider) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement RequestManageTracksListener and CurrentMapProvider");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestManageTracksListener = null;
        mCurrentMapProvider = null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            /* If there is no TileView, no need to go further */
            if (mTileView == null) {
                return;
            }

            /* In the case there is no Projection defined, the latitude and longitude are used */
            Projection projection = mMap.getProjection();
            if (projection != null) {
                ProjectionTask projectionTask = new ProjectionTask(this, location.getLatitude(),
                        location.getLongitude(), projection);
                projectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                updatePosition(location.getLongitude(), location.getLatitude());
            }
        }
    }

    @Override
    public void onProjectionUpdate(Projection projection) {
        double[] val = projection.getProjectedValues();
        updatePosition(val[0], val[1]);
    }

    /**
     * Updates the position on the {@link Map}.
     * Also, if we locked the view, we center the TileView on the current position.
     *
     * @param x the projected X coordinate, or longitude if there is no {@link Projection}
     * @param y the projected Y coordinate, or latitude if there is no {@link Projection}
     */
    private void updatePosition(double x, double y) {
        mTileView.moveMarker(mPositionMarker, x, y);

        if (mLockView) {
            centerOnPosition();
        }
    }

    private void setTileView(TileViewExtended tileView) {
        mTileView = tileView;
        mTileView.setId(R.id.tileview_id);
        mTileView.setSaveEnabled(true);
        rootView.addView(mTileView, 0);
        mTileView.setSingleTapListener(rootView);
        mTileView.setScrollListener(rootView);

        /**
         * Register the position marker as an {@link OrientationListener}
         */
        mOrientationSensor.setOrientationListener((OrientationSensor.OrientationListener) mPositionMarker);

        /* Update the marker layer */
        mMarkerLayer.setTileView(tileView);
    }

    private void removeCurrentTileView() {
        try {
            mTileView.destroy();
            rootView.removeView(mTileView);
        } catch (Exception e) {
            // don't care
        }
    }

    /**
     * Sets the map to generate a new {@link TileViewExtended}.
     *
     * @param map The new {@link Map} object
     */
    private void setMap(Map map) {
        mMap = map;
        TileViewExtended tileView = new TileViewExtended(this.getContext());

        /* Set the size of the view in px at scale 1 */
        tileView.setSize(map.getWidthPx(), map.getHeightPx());

        /* Lowest scale */
        List<MapGson.Level> levelList = map.getLevelList();
        float scale = 1 / (float) Math.pow(2, levelList.size() - 1);

        /* Scale limits */
        tileView.setScaleLimits(scale, 2);

        /* Starting scale */
        tileView.setScale(scale);

        /* DetailLevel definition */
        for (MapGson.Level level : levelList) {
            tileView.addDetailLevel(scale, level.level, level.tile_size.x, level.tile_size.y);
            /* Calculate each level scale for best precision */
            scale = 1 / (float) Math.pow(2, levelList.size() - level.level - 2);
        }

        /* Allow the scale to be no less to see the entire map */
        tileView.setMinimumScaleMode(ZoomPanLayout.MinimumScaleMode.FIT);

        /* Render while panning */
        tileView.setShouldRenderWhilePanning(true);

        /* Map calibration */
        Map.MapBounds mapBounds = map.getMapBounds();
        if (mapBounds != null) {
            tileView.defineBounds(mapBounds.projectionX0,
                    mapBounds.projectionY0,
                    mapBounds.projectionX1,
                    mapBounds.projectionY1);
        } else {
            tileView.defineBounds(0, 0, 1, 1);
        }

        /* Add a down-sample image */
//        ImageView downSampleImage = new ImageView(context);
//        downSampleImage.setImageBitmap(map.getDownSample());
//        tileView.addView(downSampleImage, 0);

        /* The BitmapProvider */
        tileView.setBitmapProvider(map.getBitmapProvider());

        /* The position reticule */
        mPositionMarker = rootView.getDetachedPositionMarker();
        tileView.addMarker(mPositionMarker, 0, 0, -0.5f, -0.5f);

        /* Remove the existing TileView, then add the new one */
        removeCurrentTileView();
        setTileView(tileView);

        /* Display all routes */
        DrawRoutesTask drawRoutesTask = new DrawRoutesTask(map, map.getMapGson().routes, tileView);
        drawRoutesTask.execute();
    }

    public void centerOnPosition() {
        mTileView.moveToMarker(mPositionMarker, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MAP_KEY, mMap);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface RequestManageTracksListener {
        void onRequestManageTracks(Map map);
    }

    /**
     * Each {@link MapGson.Route} of a {@link Map} needs to provide data in a format that the
     * {@link TileView} understands. <br>
     * This is done in an ansynctask, to ensure that this process does not hangs the UI thread.
     */
    private static class DrawRoutesTask extends AsyncTask<Void, Void, Void> {
        private Map mMap;
        private List<MapGson.Route> mRouteList;
        private WeakReference<TileViewExtended> mTileViewWeakReference;
        private WeakReference<CoordinateTranslater> mCoordinateTranslaterWeakReference;

        DrawRoutesTask(Map map, List<MapGson.Route> routeList, TileViewExtended tileView) {
            mMap = map;
            mRouteList = routeList;
            mTileViewWeakReference = new WeakReference<>(tileView);
            mCoordinateTranslaterWeakReference = new WeakReference<>(tileView.getCoordinateTranslater());
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (MapGson.Route route : mRouteList) {
                List<MapGson.Marker> markerList = route.route_markers;
                /* If there is only one marker, the path has no sense */
                if (markerList.size() < 2) continue;


                CoordinateTranslater coordinateTranslater = mCoordinateTranslaterWeakReference.get();
                if (coordinateTranslater == null) continue;

                int size = markerList.size() * 4 - 4;
                float[] lines = new float[size];

                int i = 0;
                int markerIndex = 0;
                for (MapGson.Marker marker : markerList) {
                    if (markerIndex % 2 != 0) {
                        lines[i] = (float) coordinateTranslater.translateX(marker.proj_x);
                        lines[i + 1] = (float) coordinateTranslater.translateY(marker.proj_y);
                        if (i + 2 >= size) break;
                        lines[i + 2] = lines[i];
                        lines[i + 3] = lines[i + 1];
                        i += 4;
                    } else {
                        lines[i] = (float) coordinateTranslater.translateX(marker.proj_x);
                        lines[i + 1] = (float) coordinateTranslater.translateY(marker.proj_y);
                        i += 2;
                    }
                    markerIndex++;
                }

                /* Set the route data */
                PathView.DrawablePath drawablePath = new PathView.DrawablePath(lines, null);
                route.setData(drawablePath);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            TileViewExtended tileView = mTileViewWeakReference.get();
            if (tileView != null) {
                tileView.drawRoutes(mMap.getMapGson().routes);
            }
        }
    }
}
