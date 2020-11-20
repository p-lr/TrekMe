package com.peterlaurence.trekme.ui.mapview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;

import com.peterlaurence.mapview.MapView;
import com.peterlaurence.mapview.core.CoordinateTranslater;
import com.peterlaurence.mapview.markers.MarkerTapListener;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.mapview.components.MarkerCallout;
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab;
import com.peterlaurence.trekme.ui.mapview.components.MovableMarker;
import com.peterlaurence.trekme.ui.mapview.controller.CalloutPosition;
import com.peterlaurence.trekme.ui.mapview.controller.CalloutPositionerKt;
import com.peterlaurence.trekme.ui.tools.TouchMoveListener;
import com.peterlaurence.trekme.util.MetricsKt;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.peterlaurence.mapview.api.MarkerApiKt.addCallout;
import static com.peterlaurence.mapview.api.MarkerApiKt.addMarker;
import static com.peterlaurence.mapview.api.MarkerApiKt.moveMarker;
import static com.peterlaurence.mapview.api.MarkerApiKt.removeCallout;
import static com.peterlaurence.mapview.api.MarkerApiKt.removeMarker;


/**
 * All {@link MovableMarker} and {@link MarkerCallout} are managed here. <br>
 * This object is intended to work along with a {@link MapViewFragment}.
 * After being created, the method {@link #init(Map, MapView)} has to be called.
 *
 * @author P.Laurence on 09/04/17.
 */
class MarkerLayer implements MapLoader.MapMarkerUpdateListener, MarkerTapListener {
    List<MarkerGson.Marker> mMarkers;
    private MapView mMapView;
    private Map mMap;
    private final MapLoader mMapLoader;

    MarkerLayer(MapLoader mapLoader) {
        mMapLoader = mapLoader;
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link TouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private static void attachMarkerGrab(final MovableMarker movableMarker, MapView mapView, Map map,
                                         MapLoader mapLoader) {
        /* Add a view as background, to move easily the marker */
        TouchMoveListener.MarkerMoveAgent markerMarkerMoveAgent = (mapView1, view, x, y) -> {
            moveMarker(mapView1, view, x, y);
            moveMarker(mapView1, movableMarker, x, y);
            movableMarker.setRelativeX(x);
            movableMarker.setRelativeY(y);
        };

        MarkerGrab markerGrab = new MarkerGrab(mapView.getContext());
        TouchMoveListener.ClickCallback markerClickCallback = new MovableMarkerClickCallback(
                movableMarker, markerGrab, mapView, map, mapLoader);
        TouchMoveListener touchMoveListener = new TouchMoveListener(mapView, markerMarkerMoveAgent, markerClickCallback);
        mapView.addReferentialOwner(touchMoveListener);
        markerGrab.setOnTouchMoveListener(touchMoveListener);
        addMarker(mapView, markerGrab, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f, 0f, 0f);
        markerGrab.morphIn(null);
    }

    @Override
    public void onMapMarkerUpdate() {
        drawMarkers();
    }

    private void setMapView(final MapView mapView) {
        mMapView = mapView;
    }

    @Override
    public void onMarkerTap(@NonNull View view, int x, int y) {
        if (mMapView == null) return;

        if (view instanceof MovableMarker) {
            MovableMarker movableMarker = (MovableMarker) view;

            /* Prepare the callout */
            MarkerCallout markerCallout = new MarkerCallout(mMapView.getContext());
            markerCallout.setMoveAction(new MorphMarkerRunnable(movableMarker, markerCallout,
                    mMapView, mMap, mMapLoader));
            markerCallout.setEditAction(new EditMarkerRunnable(mMap.getId(), movableMarker,
                    markerCallout, mMapView));
            markerCallout.setDeleteAction(new DeleteMarkerRunnable(movableMarker, markerCallout,
                    mMapView, mMap, mMapLoader));
            MarkerGson.Marker marker = movableMarker.getMarker();
            markerCallout.setTitle(marker.name);
            markerCallout.setSubTitle(marker.lat, marker.lon);

            int calloutHeight = MetricsKt.getPx(120);
            int markerHeight = MetricsKt.getPx(48); // The view height is 48dp, but only the top half is used to draw the marker.
            int calloutWidth = MetricsKt.getPx(200);
            int markerWidth = MetricsKt.getPx(24);
            CalloutPosition pos = CalloutPositionerKt.positionCallout(mMapView, calloutWidth, calloutHeight,
                    movableMarker.getRelativeX(), movableMarker.getRelativeY(), markerWidth, markerHeight);

            addCallout(mMapView, markerCallout, movableMarker.getRelativeX(), movableMarker.getRelativeY(),
                    pos.getRelativeAnchorLeft(), pos.getRelativeAnchorTop(), pos.getAbsoluteAnchorLeft(), pos.getAbsoluteAnchorTop());
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
        mMapLoader.setMapMarkerUpdateListener(this);

        if (mMap.areMarkersDefined()) {
            drawMarkers();
        } else {
            mMapLoader.getMarkersForMap(mMap);
        }
    }

    private void drawMarkers() {
        mMarkers = mMap.getMarkers();
        if (mMarkers == null) return;

        for (MarkerGson.Marker marker : mMarkers) {
            MovableMarker movableMarker = new MovableMarker(mMapView.getContext(), true, marker);
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
        movableMarker = new MovableMarker(mMapView.getContext(), false, newMarker);
        movableMarker.setRelativeX(relativeX);
        movableMarker.setRelativeY(relativeY);
        movableMarker.initRounded();

        if (mMap != null) {
            mMap.addMarker(newMarker);
        }

        /* Easily move the marker */
        attachMarkerGrab(movableMarker, mMapView, mMap, mMapLoader);

        addMarker(mMapView, movableMarker, relativeX, relativeY, -0.5f, -0.5f, 0f, 0f);
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
        private final MapLoader mMapLoader;

        MovableMarkerClickCallback(MovableMarker movableMarker, MarkerGrab markerGrab,
                                   MapView mapView, Map map, MapLoader mapLoader) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerGrabWeakReference = new WeakReference<>(markerGrab);
            mMapView = mapView;
            mMap = map;
            mMapLoader = mapLoader;
        }

        @Override
        public void onMarkerClick() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();
            if (movableMarker != null) {
                movableMarker.morphToStaticForm();

                /* After the morph, remove the MarkerGrab */
                final MarkerGrab markerGrab = mMarkerGrabWeakReference.get();
                markerGrab.morphOut(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (markerGrab != null) {
                            removeMarker(mMapView, markerGrab);
                            TouchMoveListener l = markerGrab.getOnTouchMoveListener();
                            if (l != null) {
                                mMapView.removeReferentialOwner(l);
                            }
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
                mMapLoader.saveMarkers(mMap);
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
        private Map mMap;
        private final MapLoader mMapLoader;

        MorphMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, MapView mapView,
                            Map map, MapLoader mapLoader) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
            mMap = map;
            mMapLoader = mapLoader;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                movableMarker.morphToDynamicForm();

                /* Easily move the marker */
                attachMarkerGrab(movableMarker, mMapView, mMap, mMapLoader);

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
     * be edited. <br>
     * Here, this component is a {@link MarkerCallout}.
     */
    private static class EditMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private MapView mMapView;
        private int mMapId;

        EditMarkerRunnable(int mapId, MovableMarker movableMarker,
                           MarkerCallout markerCallout, MapView mapView) {
            mMapId = mapId;
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                MapViewFragmentDirections.ActionMapViewFragmentToMarkerManageFragment action = MapViewFragmentDirections.actionMapViewFragmentToMarkerManageFragment(mMapId, movableMarker.getMarker());
                Navigation.findNavController(mMapView).navigate(action);
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
        private final MapLoader mMapLoader;

        DeleteMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout,
                             MapView mapView, Map map, MapLoader mapLoader) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mMapView = mapView;
            mMap = map;
            mMapLoader = mapLoader;
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
                mMapLoader.deleteMarker(mMap, marker);
            }
        }
    }
}
