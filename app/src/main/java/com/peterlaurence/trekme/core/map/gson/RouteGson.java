package com.peterlaurence.trekme.core.map.gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peterLaurence on 13/05/17.
 */
public class RouteGson {
    public List<Route> routes;

    public RouteGson() {
        routes = new ArrayList<>();
    }

    public static class Route implements Serializable {
        public String name;
        public boolean visible = true;
        public List<MarkerGson.Marker> route_markers;
        private transient Object mData;
        private final transient Object mDataLock = new Object();

        public Route() {
            route_markers = new ArrayList<>();
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
