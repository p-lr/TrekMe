package com.peterlaurence.trekme.core.map.data.models;

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

        /** Legacy routes don't have an id. For a non null id, use getCompositeId() */
        @Nullable
        public String id;

        @Nullable
        public String color; // In the form "#RRGGBB" or "#AARRGGBB"

        public boolean visible = true;
        private List<MarkerGson.Marker> route_markers;
        public boolean elevationTrusted = false;

        public Route() {
            route_markers = new ArrayList<>();
        }

        public List<MarkerGson.Marker> getRouteMarkers() {
            return route_markers;
        }

        public void setRouteMarkers(List<MarkerGson.Marker> markers) {
            route_markers = markers;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Route)) return false;
            Route r = (Route) o;
            if (id != null) return id.equals(r.id);
            return r.name.equals(name) && r.route_markers.size() == route_markers.size();
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
