package com.peterlaurence.trekadvisor.core.map.gson;

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

    public static class Route {
        public String name;
        public boolean visible;
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

        @Override
        public boolean equals(Object o) {
            return !(o == null || !(o instanceof Route)) && ((Route) o).name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
