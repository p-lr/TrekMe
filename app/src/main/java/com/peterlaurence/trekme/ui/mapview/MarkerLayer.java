package com.peterlaurence.trekme.ui.mapview;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.peterlaurence.mapview.MapView;
import com.peterlaurence.mapview.core.CoordinateTranslater;
import com.peterlaurence.mapview.markers.MarkerTapListener;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.mapview.components.MarkerCallout;
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab;
import com.peterlaurence.trekme.ui.mapview.components.MovableMarker;
import com.peterlaurence.trekme.ui.tools.TouchMoveListener;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.peterlaurence.mapview.markers.MarkerLayoutKt.addCallout;
import static com.peterlaurence.mapview.markers.MarkerLayoutKt.addMarker;
import static com.peterlaurence.mapview.markers.MarkerLayoutKt.moveMarker;
import static com.peterlaurence.mapview.markers.MarkerLayoutKt.removeCallout;
import static com.peterlaurence.mapview.markers.MarkerLayoutKt.removeMarker;

/**
 * All {@link MovableMarker} and {@link MarkerCallout} are managed here. <br>
 * This object is intended to work along with a {@link MapViewFragment}.
 *
 * @author peterLaurence on 09/04/17.
 */
class MarkerLayer implements MapLoader.MapMarkerUpdateListener, MarkerTapListener {
    List<MarkerGson.Marker> mMarkers;
    private Context mContext;
    private MapViewFragment.RequestManageMarkerListener mRequestManageMarkerListener;
    private MapView mMapView;
    private Map mMap;
    private MovableMarker mCurrentMovableMarker;


    /**
     * After being created, the method {@link #init(Map, MapView)} has to be called.
     */
    MarkerLayer(Context context) {
        mContext = context;
    }

    public void setRequestManageMarkerListener(MapViewFragment.RequestManageMarkerListener listener) {
        mRequestManageMarkerListener = listener;
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link TouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private static void attachMarkerGrab(final MovableMarker movableMarker, MapView mapView,
                                         Map map, Context context) {
        /* Add a view as background, to move easily the marker */
        TouchMoveListener.MoveCallback markerMoveCallback = new TouchMoveListener.MoveCallback() {
            @Override
            public void onMarkerMove(MapView mapView, View view, double x, double y) {
                moveMarker(mapView, view, x, y);
                moveMarker(mapView, movableMarker, x, y);
                movableMarker.setRelativeX(x);
                movableMarker.setRelativeY(y);
            }
        };

        MarkerGrab markerGrab = new MarkerGrab(context);
        TouchMoveListener.ClickCallback markerClickCallback = new MovableMarkerClickCallback(
                movableMarker, markerGrab, mapView, map);
        markerGrab.setOnTouchListener(new TouchMoveListener(mapView, markerMoveCallback, markerClickCallback));
        addMarker(mapView, markerGrab, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f, 0f, 0f);
        markerGrab.morphIn();
    }

    MarkerGson.Marker getCurrentMarker() {
        return mCurrentMovableMarker.getMarker();
    }

    private void setCurrentMarker(MovableMarker movableMarker) {
        mCurrentMovableMarker = movableMarker;
    }

    @Override
    public void onMapMarkerUpdate() {
        drawMarkers();
    }

    private void setMapView(final MapView mapView) {
        mMapView = mapView;
    }

    @Override
    public void onMarkerTap(View view, int i, int i1) {
        if (mMapView == null) return;

        if (view instanceof MovableMarker) {
            MovableMarker movableMarker = (MovableMarker) view;

            /* Prepare the callout */
            MarkerCallout markerCallout = new MarkerCallout(mContext);
            markerCallout.setMoveAction(new MorphMarkerRunnable(movableMarker, markerCallout,
                    mMapView, mContext, mMap));
            markerCallout.setEditAction(new EditMarkerRunnable(mMap.getId(), movableMarker, MarkerLayer.this,
                    markerCallout, mMapView, mRequestManageMarkerListener));
            markerCallout.setDeleteAction(new DeleteMarkerRunnable(movableMarker, markerCallout,
                    mMapView, mMap));
            MarkerGson.Marker marker = movableMarker.getMarker();
            markerCallout.setTitle(marker.name);
            markerCallout.setSubTitle(marker.lat, marker.lon);

            addCallout(mMapView, markerCallout, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -1.2f, 0f, 0f);
            markerCallout.transitionIn();
        }
    }

    /**
     * Triggers the fetch of the map's markers and their drawing on the {@link MapView}. If this is
     * the first time this method is called for this map, the markers aren't defined and the
     * {@link MapLoader} will get them in an asynctask. Otherwise, we can draw them immediately.
     * <p>
     * This must be called when the {@link MapViewFragment} is ready to update its UI.
     * <p>
     * The caller is responsible for removing this {@link MapLoader.MapMarkerUpdateListener} from
     * the {@link MapLoader}, after this object is no longer used.
     */
    public void init(Map map, MapView mapView) {
        mMap = map;
        setMapView(mapView);
        MapLoader.INSTANCE.setMapMarkerUpdateListener(this);

        if (mMap.areMarkersDefined()) {
            drawMarkers();
        } else {
            MapLoader.INSTANCE.getMarkersForMap(mMap);
        }
    }

    private void drawMarkers() {
        mMarkers = mMap.getMarkers();
        if (mMarkers == null) return;

        for (MarkerGson.Marker marker : mMarkers) {
            MovableMarker movableMarker = new MovableMarker(mContext, true, marker);
            if (mMap.getProjection() == null) {
                movableMarker.setRelativeX(marker.lon);
                movableMarker.setRelativeY(marker.lat);
            } else {
                /* Take proj values, and fallback to lat-lon if they are null */
                movableMarker.setRelativeX(marker.proj_x != null ? marker.proj_x : marker.lon);
                movableMarker.setRelativeY(marker.proj_y != null ? marker.proj_y : marker.lat);
            }
            movableMarker.initStatic();

            addMarker(mMapView, movableMarker, movableMarker.getRelativeX(),
                    movableMarker.getRelativeY(), -0.5f, -0.5f, 0f, 0f);
        }
    }

    /**
     * Add a {@link MovableMarker} to the center of the {@link MapView}.
     */
    void addNewMarker() {
        /* Calculate the relative coordinates of the center of the screen */
        int x = mMapView.getScrollX() + mMapView.getWidth() / 2 - mMapView.getOffsetX();
        int y = mMapView.getScrollY() + mMapView.getHeight() / 2 - mMapView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mMapView.getCoordinateTranslater();
        final double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mMapView.getScale());
        final double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mMapView.getScale());

        final MovableMarker movableMarker;

        /* Create a new marker and add it to the map */
        MarkerGson.Marker newMarker = new MarkerGson.Marker();

        if (mMap.getProjection() == null) {
            newMarker.lat = relativeY;
            newMarker.lon = relativeX;
        } else {
            newMarker.proj_x = relativeX;
            newMarker.proj_y = relativeY;
            double[] wgs84Coords;
            wgs84Coords = mMap.getProjection().undoProjection(relativeX, relativeY);
            if (wgs84Coords != null) {
                newMarker.lon = wgs84Coords[0];
                newMarker.lat = wgs84Coords[1];
            }
        }

        /* Create the corresponding view */
        movableMarker = new MovableMarker(mContext, false, newMarker);
        movableMarker.setRelativeX(relativeX);
        movableMarker.setRelativeY(relativeY);
        movableMarker.initRounded();

        if (mMap != null) {
            mMap.addMarker(newMarker);
        }

        /* Easily move the marker */
        attachMarkerGrab(movableMarker, mMapView, mMap, mContext);

        addMarker(mMapView, movableMarker, relativeX, relativeY, -0.5f, -0.5f, 0f, 0f);
    }

    /**
     * The {@link MarkerGson.Marker} of the {@code mCurrentMovableMarker} has changed. <br>
     * Updates the view.
     */
    void updateCurrentMarker() {
        if (mMap.getProjection() == null) {
            mCurrentMovableMarker.setRelativeX(mCurrentMovableMarker.getMarker().lon);
            mCurrentMovableMarker.setRelativeY(mCurrentMovableMarker.getMarker().lat);
        } else {
            mCurrentMovableMarker.setRelativeX(mCurrentMovableMarker.getMarker().proj_x);
            mCurrentMovableMarker.setRelativeY(mCurrentMovableMarker.getMarker().proj_y);
        }

        moveMarker(mMapView, mCurrentMovableMarker, mCurrentMovableMarker.getRelativeX(),
                mCurrentMovableMarker.getRelativeY());
    }

    /**
     * This callback is only called when a single-tap is detected on a {@link MarkerGrab} (e.g when
     * the associated {@link MovableMarker} can be moved). <br>
     * So it does the following :
     * <ul>
     * <li>Morph the {@link MovableMarker} into its static form</li>
     * <li>Animate out and remove the {@link MarkerGrab} which help the user to move the
     * {@link MovableMarker}</li>
     * <li>Update the {@link MarkerGson.Marker} associated with the relative coordinates of the
     * {@link MovableMarker}. Depending on the {@link Map} using a projection or not, those
     * relative coordinates are wgs84 or projected values.</li>
     * </ul>
     */
    private static class MovableMarkerClickCallback implements TouchMoveListener.ClickCallback {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerGrab> mMarkerGrabWeakReference;
        private MapView mMapView;
        private Map mMap;

        MovableMarkerClickCallback(MovableMarker movableMarker, MarkerGrab markerGrab,
                                   MapView mapView, Map map) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerGrabWeakReference = new WeakReference<>(markerGrab);
            mMapView = mapView;
            mMap = map;
        }

        @Override
        public void onMarkerClick() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();
            if (movableMarker != null) {
                movableMarker.morphToStaticForm();

                /* After the morph, remove the MarkerGrab */
                final MarkerGrab markerGrab = mMarkerGrabWeakReference.get();
                markerGrab.morphOut(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        super.onAnimationEnd(drawable);
                        if (markerGrab != null) {
                            removeMarker(mMapView, markerGrab);
                        }
                    }
                });

                /* The view has been moved, update the associated model object */
                MarkerGson.Marker marker = movableMarker.getMarker();
                if (mMap.getProjection() == null) {
                    marker.lon = movableMarker.getRelativeX();
                    marker.lat = movableMarker.getRelativeY();
                } else {
                    marker.proj_x = movableMarker.getRelativeX();
                    marker.proj_y = movableMarker.getRelativeY();
                    double[] wgs84Coords;
                    wgs84Coords = mMap.getProjection().undoProjection(marker.proj_x, marker.proj_y);
                    if (wgs84Coords != null) {
                        marker.lon = wgs84Coords[0];
                        marker.lat = wgs84Coords[1];
                    }
                }

                /* Save the changes on the markers.json file */
                MapLoader.INSTANCE.saveMarkers(mMap);
            }
        }
    }

    /**
     * This runnable is called when an external component requests a {@link MovableMarker} to
     * morph into the dynamic shape. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class MorphMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private MapView mMapView;
        private Context mContext;
        private Map mMap;

        MorphMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, MapView mapView,
                            Context context, Map map) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
            mContext = context;
            mMap = map;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                movableMarker.morphToDynamicForm();

                /* Easily move the marker */
                attachMarkerGrab(movableMarker, mMapView, mMap, mContext);

                /* Use a trick to bring the marker to the foreground */
                removeMarker(mMapView, movableMarker);
                addMarker(mMapView, movableMarker, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f, 0f, 0f);
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                removeCallout(mMapView, markerCallout);
            }
        }
    }

    /**
     * This runnable is called when an external component requests a {@link MovableMarker} to
     * be edited. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class EditMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerLayer> mMarkerLayerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private MapView mMapView;
        private WeakReference<MapViewFragment.RequestManageMarkerListener> mListenerWeakRef;
        private int mMapId;

        EditMarkerRunnable(int mapId, MovableMarker movableMarker, MarkerLayer markerLayer,
                           MarkerCallout markerCallout, MapView mapView,
                           MapViewFragment.RequestManageMarkerListener listener) {
            mMapId = mapId;
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerLayerWeakReference = new WeakReference<>(markerLayer);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
            mListenerWeakRef = new WeakReference<>(listener);
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                if (mListenerWeakRef != null) {
                    MapViewFragment.RequestManageMarkerListener listener = mListenerWeakRef.get();
                    if (listener != null) {
                        MarkerLayer markerLayer = mMarkerLayerWeakReference.get();
                        if (markerLayer != null) {
                            markerLayer.setCurrentMarker(movableMarker);
                        }

                        listener.onRequestManageMarker(mMapId, movableMarker.getMarker());
                    }
                }
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                removeCallout(mMapView, markerCallout);
            }
        }
    }

    /**
     * This runnable is called when an external component requests a {@link MovableMarker} to
     * be deleted. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class DeleteMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private MapView mMapView;
        private Map mMap;

        DeleteMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout,
                             MapView mapView, Map map) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
            mMap = map;
        }

        @Override
        public void run() {
            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                removeCallout(mMapView, markerCallout);
            }

            /* Delete the marker */
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                removeMarker(mMapView, movableMarker);

                MarkerGson.Marker marker = movableMarker.getMarker();
                MapLoader.INSTANCE.deleteMarker(mMap, marker);
            }
        }
    }
}
