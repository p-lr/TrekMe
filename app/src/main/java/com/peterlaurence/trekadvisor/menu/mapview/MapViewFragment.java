package com.peterlaurence.trekadvisor.menu.mapview;

import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.core.projection.Projection;
import com.peterlaurence.trekadvisor.core.projection.ProjectionTask;
import com.peterlaurence.trekadvisor.core.sensors.OrientationSensor;
import com.peterlaurence.trekadvisor.menu.LocationProvider;
import com.peterlaurence.trekadvisor.menu.MapProvider;
import com.peterlaurence.trekadvisor.menu.mapview.components.tracksmanage.TracksManageFragment;
import com.qozix.tileview.widgets.ZoomPanLayout;

import java.util.List;

/**
 * A {@link Fragment} subclass that implements required interfaces to be used with a
 * {@link GoogleApiClient}.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link RequestManageTracksListener} and {@link MapProvider} interfaces to handle
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
        FrameLayoutMapView.LockViewListener {

    public static final String TAG = "MapViewFragment";
    private FrameLayoutMapView rootView;
    private TileViewExtended mTileView;
    private Map mMap;
    private View mPositionMarker;
    private boolean mLockView = false;
    private RequestManageTracksListener mRequestManageTracksListener;
    private RequestManageMarkerListener mRequestManageMarkerListener;
    private MapProvider mMapProvider;
    private LocationProvider mLocationProvider;
    private LocationRequest mLocationRequest;
    private OrientationSensor mOrientationSensor;
    private MarkerLayer mMarkerLayer;
    private RouteLayer mRouteLayer;
    private DistanceLayer mDistanceLayer;
    private SpeedListener mSpeedListener;
    private DistanceLayer.DistanceListener mDistanceListener;

    public MapViewFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RequestManageTracksListener
                && context instanceof RequestManageMarkerListener
                && context instanceof MapProvider
                && context instanceof LocationProvider) {
            mRequestManageTracksListener = (RequestManageTracksListener) context;
            mRequestManageMarkerListener = (RequestManageMarkerListener) context;
            mMapProvider = (MapProvider) context;
            mLocationProvider = (LocationProvider) context;
            mLocationProvider.registerLocationListener(this, this);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement RequestManageTracksListener, MapProvider and LocationProvider");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        /* The location request specific to this fragment */
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Create layout from scratch if it does not exist, else don't re-create the TileView,
         * it handles configuration changes itself
         */
        if (rootView == null) {
            rootView = new FrameLayoutMapView(getContext());
            rootView.setPositionTouchListener(this);
            rootView.setLockViewListener(this);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* Get the speed and distances indicators from the main layout */
        mSpeedListener = rootView.getSpeedIndicator();
        mDistanceListener = rootView.getDistanceIndicator();

        /* Create the instance of the OrientationSensor */
        if (mOrientationSensor == null) {
            mOrientationSensor = new OrientationSensor(getActivity());
        }

        /* Create the marker layer */
        if (mMarkerLayer == null) {
            mMarkerLayer = new MarkerLayer(view, getContext());
        }
        mMarkerLayer.setRequestManageMarkerListener(mRequestManageMarkerListener);

        /* Create the route layer */
        if (mRouteLayer == null) {
            mRouteLayer = new RouteLayer();
        }

        /* Create the distance layer */
        if (mDistanceLayer == null) {
            mDistanceLayer = new DistanceLayer(getContext(), mDistanceListener);
        }
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

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_view, menu);

        /* .. and restore some checkable state */
        MenuItem item = menu.findItem(R.id.distancemeter_id);
        item.setChecked(mDistanceLayer.isVisible());

        MenuItem itemOrientation = menu.findItem(R.id.orientation_enable_id);
        itemOrientation.setChecked(mOrientationSensor.isStarted());

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_marker_id:
                mMarkerLayer.addNewMarker();
                return true;
            case R.id.manage_tracks_id:
                mRequestManageTracksListener.onRequestManageTracks();
                return true;
            case R.id.speedometer_id:
                mSpeedListener.toggleSpeedVisibility();
                return true;
            case R.id.distancemeter_id:
                mDistanceListener.toggleDistanceVisibility();
                item.setChecked(!item.isChecked());
                if (item.isChecked()) {
                    mDistanceLayer.show();
                } else {
                    mDistanceLayer.hide();
                }
                return true;
            case R.id.orientation_enable_id:
                item.setChecked(mOrientationSensor.toggleOrientation());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPositionTouch() {
        mTileView.setScale(1f);
        centerOnPosition();
    }

    @Override
    public void onLockView(boolean lock) {
        mLockView = lock;
    }

    @Override
    public void onStart() {
        super.onStart();

        updateMapIfNecessary();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mSpeedListener.hideSpeed();
            mDistanceLayer.hide();
        } else {
            updateMapIfNecessary();
        }
    }

    /**
     * Only update the map if its a new one. <br>
     * Once the map is updated, a {@link TileViewExtended} instance is created, so layers can be
     * updated.
     */
    private void updateMapIfNecessary() {
        Map map = mMapProvider.getCurrentMap();
        if (map != null && mMap != map) {
            setMap(map);
            updateLayers();
        }
    }

    private void updateLayers() {
        /* Update the marker layer */
        mMarkerLayer.init(mMap, mTileView);

        /* Update the route layer */
        mRouteLayer.init(mMap, mTileView);

        /* Update the distance layer */
        mDistanceLayer.init(mMap, mTileView);
    }

    @Override
    public void onStop() {
        mLocationProvider.removeLocationListener(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        MapLoader.getInstance().clearMapMarkerUpdateListener();
        MapLoader.getInstance().clearMapRouteUpdateListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mRequestManageTracksListener = null;
        mRequestManageMarkerListener = null;
        mMapProvider = null;
        mSpeedListener = null;
        mOrientationSensor.stop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationProvider.requestLocationUpdates(this, mLocationRequest);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (isHidden()) return;

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

            /* If the user wants to see the speed */
            if (mSpeedListener != null) {
                mSpeedListener.onSpeed(location.getSpeed(), SpeedUnit.KM_H);
            }
        }
    }

    @Override
    public void onProjectionUpdate(double[] projectedValues) {
        updatePosition(projectedValues[0], projectedValues[1]);
    }

    public MarkerGson.Marker getCurrentMarker() {
        return mMarkerLayer.getCurrentMarker();
    }

    public void currentMarkerEdited() {
        mMarkerLayer.updateCurrentMarker();
    }

    public TracksManageFragment.TrackChangeListener getTrackChangeListener() {
        return mRouteLayer;
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
            tileView.defineBounds(mapBounds.X0,
                    mapBounds.Y0,
                    mapBounds.X1,
                    mapBounds.Y1);
        } else {
            tileView.defineBounds(0, 0, 1, 1);
        }

        /* The BitmapProvider */
        tileView.setBitmapProvider(map.getBitmapProvider());

        /* The position reticule */
        mPositionMarker = rootView.getDetachedPositionMarker();
        tileView.addMarker(mPositionMarker, 0, 0, -0.5f, -0.5f);

        /* Remove the existing TileView, then add the new one */
        removeCurrentTileView();
        setTileView(tileView);
    }

    public void centerOnPosition() {
        mTileView.moveToMarker(mPositionMarker, true);
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
        void onRequestManageTracks();
    }

    /**
     * Same as {@link RequestManageTracksListener}.
     */
    public interface RequestManageMarkerListener {
        void onRequestManageMarker(MarkerGson.Marker marker);
    }

    public enum SpeedUnit {
        KM_H, MPH
    }

    /**
     * As the {@code MapViewFragment} is a {@link LocationListener}, it can dispatch speed
     * information to other sub-components.
     */
    public interface SpeedListener {
        void onSpeed(float speed, SpeedUnit unit);

        void toggleSpeedVisibility();

        void hideSpeed();
    }
}
