package com.peterlaurence.trekme.core.map.gson;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author P.Laurence on 13/05/17.
 */
public class RouteGson {
    public List<Route> routes;

    public RouteGson() {
        routes = new ArrayList<>();
    }

    public static class Route implements Serializable {
        public String name;
        @Nullable
        public String color; // In the form "#RRGGBB" or "#AARRGGBB"
        public boolean visible = true;
        private List<MarkerGson.Marker> route_markers;
        private transient Object mData;
        private final transient Object mDataLock = new Object();
        public boolean elevationTrusted = false;

        public Route() {
            route_markers = new ArrayList<>();
        }

        /**
         * Keep in mind that iterating the list of markers should be done while holding the monitor
         * of {@link #route_markers}, especially when new markers are concurrently added to this
         * route.
         */
        public List<MarkerGson.Marker> getRouteMarkers() {
            synchronized (this) {
                return route_markers;
            }
        }

        public void addMarker(MarkerGson.Marker marker) {
            synchronized (this) {
                route_markers.add(marker);
            }
        }

        public Object getData() {
            synchronized (mDataLock) {
                return mData;
            }
        }

        public void setData(Object data) {
            synchronized (mDataLock) {
                mData = data;
            }
        }

        public void copyRoute(Route route) {
            name = route.name;
            visible = route.visible;
            route_markers = route.route_markers;
            elevationTrusted = route.elevationTrusted;
        }

        public void toggleVisibility() {
            visible = !visible;
        }

        public int getId() {
            return name.hashCode() + 31 * route_markers.size();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Route && ((Route) o).getId() == getId();
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
