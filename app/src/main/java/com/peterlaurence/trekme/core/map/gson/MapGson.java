package com.peterlaurence.trekme.core.map.gson;

import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.mapsource.WmtsSource;
import com.peterlaurence.trekme.core.projection.Projection;

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
 * @author P.Laurence
 */
public class MapGson {
    public String name;
    public String thumbnail;
    public List<Level> levels;
    public Provider provider;
    public MapSize size;
    public Calibration calibration;
    public Long sizeInBytes;


    public MapGson() {
        levels = new ArrayList<>();
        calibration = new Calibration();
    }

    public static class MapSize {
        public int x;
        public int y;
    }

    public static class Calibration {
        public Projection projection;
        public String calibration_method;
        private final List<CalibrationPoint> calibration_points;

        /**
         * A CalibrationPoint defines a point on the map whose (x, y) relative coordinates
         * correspond to (projectionX, projectionY) as projected coordinates.
         * Values of {@code x} and {@code y} are in [0-1] interval. <br>
         * Values of {@code proj_x} and {@code proj_y} can very well be bare latitude and longitude.
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

        public void addCalibrationPoint(CalibrationPoint p) {
            calibration_points.add(p);
        }

        public List<CalibrationPoint> getCalibrationPoints() {
            return new ArrayList<>(calibration_points);
        }

        public void setCalibrationPoints(List<CalibrationPoint> points) {
            calibration_points.clear();
            calibration_points.addAll(points);
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

    public static class Provider {
        public Map.MapOrigin generated_by;
        public WmtsSource wmts_source;
        public String layer_real_name;
        public String image_extension;
    }
}
