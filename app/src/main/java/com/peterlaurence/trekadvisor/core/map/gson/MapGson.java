package com.peterlaurence.trekadvisor.core.map.gson;

import com.peterlaurence.trekadvisor.core.projection.Projection;

import java.util.ArrayList;
import java.util.List;

/**
 * A POJO that is the corresponding Java Object of a map's json file.
 * It is used with Gson to :
 * <ul>
 *     <li>Serialize into a json file</li>
 *     <li>Deserialize an appropriate JSON string</li>
 * </ul>
 *
 * It is deserialized as follows :
 * <pre>
 * {@code Gson gson = new GsonBuilder().serializeNulls().create();
 *  MapGson mapGson = gson.fromJson(jsonString, MapGson.class);
 * }
 * </pre>
 * Every attribute is public, to avoid boilerplate setters/getters (so use with care). Their names
 * are identical to their json counterparts.
 *
 * @author peterLaurence
 */
public class MapGson {
    public String name;
    public String thumbnail;
    public List<Level> levels;
    public Provider provider;
    public MapSize size;
    public Calibration calibration;
    public List<Route> routes;
    public List<Marker> markers;

    public MapGson() {
        levels = new ArrayList<>();
        markers = new ArrayList<>();
        routes = new ArrayList<>();
    }

    public static class MapSize {
        public int x;
        public int y;
    }

    public static class Calibration {
        public Projection projection;
        public String calibration_method;
        public List<CalibrationPoint> calibration_points;

        /**
         * A CalibrationPoint defines a point on the map whose (x, y) relative coordinates
         * correspond to (projectionX, projectionY) as projected coordinates.
         * Values of x and y are in [0-1] interval.
         * <p/>
         * For example, a point which has x=1 and y=1 is located at the bottom right corner of the map.
         * A point which has x=0 and y=0 is located at the top left corner of the map.
         */
        public static class CalibrationPoint {
            public double x;
            public double y;
            public double proj_x;
            public double proj_y;
        }

        public Calibration() {
            calibration_points = new ArrayList<>();
        }
    }

    public static class Level {
        public int level;
        public TileSize tile_size;

        public static class TileSize {
            public int x;
            public int y;
        }
    }

    public static class Route {
        public String name;
        public boolean visible;
        public List<Marker> route_markers;
        private transient Object mData;

        public Route() {
            route_markers = new ArrayList<>();
        }

        public Object getData() {
            return mData;
        }

        public void setData(Object data) {
            mData = data;
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
            if (o == null || !(o instanceof Route)) return false;
            return ((Route) o).name.equals(this.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    public static class Marker {
        public String name;
        public double lat;
        public double lon;
        public Double proj_x;
        public Double proj_y;
        public String comment;
    }

    public static class Provider {
        public String generated_by;
        public String image_extension;
    }
}
