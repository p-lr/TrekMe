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
    public List<Track> tracks;
    public List<Marker> markers;

    public MapGson() {
        levels = new ArrayList<>();
        markers = new ArrayList<>();
        tracks = new ArrayList<>();
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

    public static class Track {
        public String name;
        public boolean visible;
        public List<Marker> track_markers;

        public Track() {
            track_markers = new ArrayList<>();
        }

        public void setVisibility(boolean visible_) {
            visible = visible_;
        }
    }

    public static class Marker {
        public String name;
        public List<Double> pos;
        public String comment;

        public Marker(){
            pos = new ArrayList<>();
        }
    }

    public static class Provider {
        public String generated_by;
        public String image_extension;
    }
}
