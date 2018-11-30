package com.peterlaurence.trekme.ui.mapview;

import android.os.AsyncTask;
import android.util.Log;

import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.mapview.components.PathView;
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * All {@link com.peterlaurence.trekme.core.map.gson.RouteGson.Route} are managed here. <br>
 * This object is intended to be used exclusively by the {@link MapViewFragment}. <p>
 * After being created, the method {@link #init(Map, TileView)} has to be called.
 *
 * @author peterLaurence on 13/05/17.
 */
class RouteLayer implements TracksManageFragment.TrackChangeListener, MapLoader.MapRouteUpdateListener {
    private static final String TAG = "RouteLayer";
    private TileViewExtended mTileView;
    private Map mMap;

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * {@link RouteGson.Route} are added to the {@link Map}.
     *
     * @param map       the {@link Map} associated with the change
     * @param routeList a list of {@link RouteGson.Route}
     */
    @Override
    public void onTrackChanged(Map map, List<RouteGson.Route> routeList) {
        Log.d(TAG, routeList.size() + " new route received for map " + map.getName());

        DrawRoutesTask drawRoutesTask = new DrawRoutesTask(map, routeList, mTileView);
        drawRoutesTask.execute();
    }

    /**
     * When saved routes are retrieved from the route.json file, this method is called.
     */
    @Override
    public void onMapRouteUpdate() {
        drawRoutes();
    }

    @Override
    public void onTrackVisibilityChanged() {
        PathView pathView = mTileView.getPathView();
        if (pathView != null) {
            pathView.invalidate();
        }
    }


    /**
     * This must be called when the {@link MapViewFragment} is ready to update its UI.
     * <p>
     * The caller is responsible for removing this {@link MapLoader.MapRouteUpdateListener} from the
     * {@link MapLoader}, after this object is no longer used.
     */
    public void init(Map map, TileView tileView) {
        mMap = map;
        setTileView((TileViewExtended) tileView);
        MapLoader.getInstance().setMapRouteUpdateListener(this);

        if (mMap.areRoutesDefined()) {
            drawRoutes();
        } else {
            MapLoader.getInstance().getRoutesForMap(mMap);
        }
    }

    private void drawRoutes() {
        /* Display all routes */
        DrawRoutesTask drawRoutesTask = new DrawRoutesTask(mMap, mMap.getRoutes(), mTileView);
        drawRoutesTask.execute();
    }

    private void setTileView(TileViewExtended tileView) {
        mTileView = tileView;
    }

    /**
     * Each {@link RouteGson.Route} of a {@link Map} needs to provide data in a format that the
     * {@link TileView} understands. <br>
     * This is done in an ansynctask, to ensure that this process does not hangs the UI thread.
     */
    private static class DrawRoutesTask extends AsyncTask<Void, Void, Void> {
        private Map mMap;
        private List<WeakReference<RouteGson.Route>> mRouteList;
        private WeakReference<TileViewExtended> mTileViewWeakReference;
        private WeakReference<CoordinateTranslater> mCoordinateTranslaterWeakReference;

        /**
         * During this task, data is generated from the markers of each route of a map. As this is
         * done in a different thread than the ui-thread (where the user is able to add/remove and
         * also modify routes), we want to avoid {@link java.util.ConcurrentModificationException}
         * when iterating over the list of routes. So we create another list of
         * {@link WeakReference< RouteGson.Route >}, while being aware that a {@link RouteGson.Route} can
         * be deleted at any time.
         */
        DrawRoutesTask(Map map, List<RouteGson.Route> routeList, TileViewExtended tileView) {
            mMap = map;

            mRouteList = new ArrayList<>();
            for (RouteGson.Route route : routeList) {
                mRouteList.add(new WeakReference<>(route));
            }

            mTileViewWeakReference = new WeakReference<>(tileView);
            mCoordinateTranslaterWeakReference = new WeakReference<>(tileView.getCoordinateTranslater());
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (WeakReference<RouteGson.Route> route : mRouteList) {
                try {
                    /* Work on a copy of the list of markers */
                    List<MarkerGson.Marker> markerList = new ArrayList<>(route.get().route_markers);
                    /* If there is only one marker, the path has no sense */
                    if (markerList.size() < 2) continue;


                    CoordinateTranslater coordinateTranslater = mCoordinateTranslaterWeakReference.get();
                    if (coordinateTranslater == null) continue;

                    int size = markerList.size() * 4 - 4;
                    float[] lines = new float[size];

                    int i = 0;
                    boolean init = true;
                    boolean mapUsesProjection = mMap.getProjection() != null;
                    for (MarkerGson.Marker marker : markerList) {
                        /* No need to continue if the route has been deleted in the meanwhile */
                        if (route.get() == null) break;

                        double relativeX = mapUsesProjection ? marker.proj_x : marker.lon;
                        double relativeY = mapUsesProjection ? marker.proj_y : marker.lat;
                        if (init) {
                            lines[i] = (float) coordinateTranslater.translateX(relativeX);
                            lines[i + 1] = (float) coordinateTranslater.translateY(relativeY);
                            init = false;
                            i += 2;
                        } else {
                            lines[i] = (float) coordinateTranslater.translateX(relativeX);
                            lines[i + 1] = (float) coordinateTranslater.translateY(relativeY);
                            if (i + 2 >= size) break;
                            lines[i + 2] = lines[i];
                            lines[i + 3] = lines[i + 1];
                            i += 4;
                        }
                    }

                    /* Set the route data */
                    PathView.DrawablePath drawablePath = new PathView.DrawablePath(lines, null);
                    route.get().setData(drawablePath);
                } catch (Exception e) {
                    // ignore and continue the loop
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            TileViewExtended tileView = mTileViewWeakReference.get();
            if (tileView != null) {
                tileView.drawRoutes(mMap.getRoutes());
            }
        }
    }
}
