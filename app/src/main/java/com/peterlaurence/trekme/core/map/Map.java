package com.peterlaurence.trekme.core.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.peterlaurence.trekme.core.map.gson.Landmark;
import com.peterlaurence.trekme.core.map.gson.LandmarkGson;
import com.peterlaurence.trekme.core.map.gson.MapGson;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.core.mapsource.WmtsSource;
import com.peterlaurence.trekme.core.projection.Projection;
import com.peterlaurence.trekme.util.ZipProgressionListener;
import com.peterlaurence.trekme.util.ZipTaskKt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.peterlaurence.trekme.core.map.ConstantsKt.MAP_FILENAME;

/**
 * A {@code Map} contains all the information that defines a map. That includes :
 * <ul>
 * <li>The name that will appear in the map choice list </li>
 * <li>The directory that contains image data and configuration file </li>
 * <li>The calibration method along with calibration points </li>
 * <li>Points of interest </li>
 * ...
 * </ul>
 *
 * <b>Warning</b>: This class isn't thread-safe. It's advised to thread-confine the use of this
 * class to the main thread.
 *
 * @author P.Laurence
 */
public class Map {
    private static final int THUMBNAIL_SIZE = 256;
    private static final String THUMBNAIL_NAME = "image.jpg";
    /* The configuration file of the map, named map.json */
    private File mConfigFile;
    private Bitmap mImage;
    private MapBounds mMapBounds;
    private boolean isFavorite = false;
    /* The Java Object corresponding to the json configuration file */
    private final MapGson mMapGson;
    /* The Java Object corresponding to the json file of markers */
    private MarkerGson mMarkerGson;
    /* The Java Object corresponding to the json file of routes */
    private RouteGson mRouteGson;
    /* The Java Object corresponding to the json file of landmarks */
    private LandmarkGson mLandmarkGson;
    private CalibrationStatus mCalibrationStatus = CalibrationStatus.NONE;

    /**
     * To create a {@link Map}, three parameters are needed. <br>
     * The {@link MarkerGson} is set later when the user wants to see the map.
     *
     * @param mapGson   the {@link MapGson} object that includes informations relative to levels,
     *                  the tile size, the name of the map, etc.
     * @param jsonFile  the {@link File} for serialization.
     * @param thumbnail the {@link File} image for map customization.
     */
    public Map(MapGson mapGson, File jsonFile, File thumbnail) {
        mMapGson = mapGson;
        mMarkerGson = new MarkerGson();
        mLandmarkGson = new LandmarkGson(new ArrayList<>());
        mRouteGson = new RouteGson();
        mConfigFile = jsonFile;
        mImage = getBitmapFromFile(thumbnail);
    }

    private static
    @Nullable
    Bitmap getBitmapFromFile(File file) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        if (file != null) {
            return BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
        }
        return null;
    }

    public
    @Nullable
    String getProjectionName() {
        if (mMapGson.calibration != null && mMapGson.calibration.projection != null) {
            return mMapGson.calibration.projection.getName();
        }
        return null;
    }

    public
    @Nullable
    Projection getProjection() {
        try {
            return mMapGson.calibration.projection;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void setProjection(Projection projection) {
        mMapGson.calibration.projection = projection;
    }

    /**
     * Get the bounds the map. See {@link MapBounds}.
     */
    public
    @Nullable
    MapBounds getMapBounds() {
        return mMapBounds;
    }

    /**
     * Check whether the map contains a given location. It's the responsibility of the caller to
     * know whether projected coordinated or lat/lon should be used.
     *
     * @param x a projected coordinate, or longitude
     * @param y a projected coordinate, or latitude
     */
    public boolean containsLocation(double x, double y) {
        if (mMapBounds != null) {
            return x >= mMapBounds.X0 && x <= mMapBounds.X1 && y <= mMapBounds.Y0 && y >= mMapBounds.Y1;
        } else {
            return false;
        }
    }

    public void calibrate() {
        /* Init the projection */
        if (mMapGson.calibration.projection != null) {
            mMapGson.calibration.projection.init();
        }

        List<MapGson.Calibration.CalibrationPoint> calibrationPoints = mMapGson.calibration.getCalibrationPoints();
        if (calibrationPoints == null) return;
        switch (getCalibrationMethod()) {
            case SIMPLE_2_POINTS:
                if (calibrationPoints.size() >= 2) {
                    /* Correct points if necessary */
                    CalibrationMethods.sanityCheck2PointsCalibration(calibrationPoints.get(0),
                            calibrationPoints.get(1));

                    mMapBounds = CalibrationMethods.simple2PointsCalibration(calibrationPoints.get(0),
                            calibrationPoints.get(1));
                }
                break;
            case CALIBRATION_3_POINTS:
                if (calibrationPoints.size() >= 3) {
                    mMapBounds = CalibrationMethods.calibrate3Points(calibrationPoints.get(0),
                            calibrationPoints.get(1), calibrationPoints.get(2));
                }
                break;
            case CALIBRATION_4_POINTS:
                if (calibrationPoints.size() == 4) {
                    mMapBounds = CalibrationMethods.calibrate4Points(calibrationPoints.get(0),
                            calibrationPoints.get(1), calibrationPoints.get(2),
                            calibrationPoints.get(3));
                }
                break;
            default:
                // don't care
        }

        /* Update the calibration status */
        setCalibrationStatus();
    }

    private void setCalibrationStatus() {
        // TODO : implement the detection of an erroneous calibration
        if (mMapGson.calibration.getCalibrationPoints().size() >= 2) {
            mCalibrationStatus = CalibrationStatus.OK;
        } else {
            mCalibrationStatus = CalibrationStatus.NONE;
        }
    }

    /**
     * @return the {@link File} which is the folder containing the map.
     */
    public final File getDirectory() {
        return mConfigFile.getParentFile();
    }

    /**
     * When the directory changed (after e.g a rename), the config file must be updated.
     */
    public void setDirectory(File dir) {
        mConfigFile = new File(dir, MAP_FILENAME);
    }

    public String getName() {
        return mMapGson.name;
    }

    public void setName(String newName) {
        mMapGson.name = newName;
    }

    public String getDescription() {
        return "";
    }

    /**
     * The calibration status is either : <ul>
     * <li>{@link CalibrationStatus#OK}</li>
     * <li>{@link CalibrationStatus#NONE}</li>
     * <li>{@link CalibrationStatus#ERROR}</li>
     * </ul>.
     */
    public CalibrationStatus getCalibrationStatus() {
        return mCalibrationStatus;
    }

    /**
     * Add a new route to the map.
     */
    public void addRoute(RouteGson.Route route) {
        if (mRouteGson != null) {
            mRouteGson.routes.add(route);
        }
    }

    /**
     * Add a new marker.
     */
    public void addMarker(MarkerGson.Marker marker) {
        mMarkerGson.markers.add(marker);
    }

    /**
     * Add a new landmark
     */
    public void addLandmark(Landmark landmark) {
        mLandmarkGson.getLandmarks().add(landmark);
    }

    public Bitmap getImage() {
        return mImage;
    }

    public int getThumbnailSize() {
        return THUMBNAIL_SIZE;
    }

    public OutputStream getImageOutputStream() {
        File targetFile = new File(getDirectory(), THUMBNAIL_NAME);
        try {
            return new FileOutputStream(targetFile);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public void setImage(Bitmap thumbnail) {
        mImage = thumbnail;
        mMapGson.thumbnail = THUMBNAIL_NAME;
    }

    public List<MapGson.Level> getLevelList() {
        return mMapGson.levels;
    }

    public MapLoader.CalibrationMethod getCalibrationMethod() {
        return MapLoader.CalibrationMethod.Companion.fromCalibrationName(
                mMapGson.calibration.calibration_method);
    }

    public void setCalibrationMethod(MapLoader.CalibrationMethod method) {
        mMapGson.calibration.calibration_method = method.name();
    }

    @Nullable
    public MapOrigin getOrigin() {
        try {
            return mMapGson.provider.generated_by;
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * A map can have several origins. Like it can come from a WMTS source, or produced using libvips.
     */
    public enum MapOrigin {
        IGN_LICENSED,  // special IGN WMTS source
        WMTS,
        VIPS,   // Custom map
    }

    @Nullable
    public WmtsSource getWmtsSource() {
        try {
            return mMapGson.provider.wmts_source;
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Returns the real name of the layer
     */
    @Nullable
    public String getLayer() {
        try {
            return mMapGson.provider.layer_real_name;
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the number of calibration that should be defined.
     */
    public int getCalibrationPointsNumber() {
        switch (getCalibrationMethod()) {
            case SIMPLE_2_POINTS:
                return 2;
            case CALIBRATION_3_POINTS:
                return 3;
            case CALIBRATION_4_POINTS:
                return 4;
            default:
                return 0;
        }
    }

    /**
     * Get a copy of the calibration points. <br>
     * This returns only a copy to ensure that no modification is made to the calibration points
     * through this call.
     */
    public List<MapGson.Calibration.CalibrationPoint> getCalibrationPoints() {
        return mMapGson.calibration.getCalibrationPoints();
    }

    public void addCalibrationPoint(MapGson.Calibration.CalibrationPoint point) {
        mMapGson.calibration.addCalibrationPoint(point);
    }

    public void setCalibrationPoints(List<MapGson.Calibration.CalibrationPoint> points) {
        mMapGson.calibration.setCalibrationPoints(points);
    }

    public String getImageExtension() {
        return mMapGson.provider.image_extension;
    }

    public int getWidthPx() {
        return mMapGson.size.x;
    }

    public int getHeightPx() {
        return mMapGson.size.y;
    }

    //TODO: Remove this. mMapGson should be private
    public final MapGson getMapGson() {
        return mMapGson;
    }

    public final MarkerGson getMarkerGson() {
        return mMarkerGson;
    }

    public void setMarkerGson(MarkerGson markerGson) {
        mMarkerGson = markerGson;
    }

    public final RouteGson getRouteGson() {
        return mRouteGson;
    }

    public void setRouteGson(RouteGson routeGson) {
        mRouteGson = routeGson;
    }

    public final LandmarkGson getLandmarkGson() {
        return mLandmarkGson;
    }

    public void setLandmarkGson(LandmarkGson landmarkGson) {
        mLandmarkGson = landmarkGson;
    }

    public boolean areMarkersDefined() {
        return mMarkerGson != null && mMarkerGson.markers.size() > 0;
    }

    public boolean areLandmarksDefined() {
        return mLandmarkGson != null && mLandmarkGson.getLandmarks().size() > 0;
    }

    public boolean areRoutesDefined() {
        return mRouteGson != null && mRouteGson.routes.size() > 0;
    }

    @Nullable
    public List<MarkerGson.Marker> getMarkers() {
        if (mMarkerGson != null) {
            return mMarkerGson.markers;
        }
        return null;
    }

    @Nullable
    public List<RouteGson.Route> getRoutes() {
        if (mRouteGson != null) {
            return mRouteGson.routes;
        }
        return null;
    }

    public final File getConfigFile() {
        return mConfigFile;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    /**
     * For instance, the {@link MapLoader} is designed so that two different maps can't have the
     * same config file path.
     *
     * @return the unique id that identifies the {@link Map}. It can later be used to retrieve the
     * {@link Map} instance from the {@link MapLoader}.
     */
    public int getId() {
        return mConfigFile.getPath().hashCode();
    }

    /**
     * Archives the map. <p>
     * Creates a zip file named with this {@link Map} name and the date. This file is placed in the
     * parent folder of the {@link Map}.
     * Beware that this is a blocking call and should be executed from inside a background thread.
     */
    public void zip(ZipProgressionListener listener, OutputStream outputStream) {
        File mapFolder = mConfigFile.getParentFile();
        if (mapFolder != null) {
            ZipTaskKt.zipTask(mConfigFile.getParentFile(), outputStream, listener);
        }
    }

    public String generateNewNameWithDate() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH);
        return getName() + "-" + dateFormat.format(date);
    }

    public enum CalibrationStatus {
        OK, NONE, ERROR
    }

    /**
     * Two {@link Map} are considered identical if they have the same configuration file.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) return false;
        if (obj instanceof Map) {
            Map objMap = (Map) obj;
            if (mConfigFile != null && objMap.getConfigFile() != null) {
                return objMap.getConfigFile().equals(getConfigFile());
            }
        }
        return false;
    }

    public boolean equals(Map map) {
        if (map == null) return false;
        return mConfigFile.equals(map.getConfigFile());
    }

    /**
     * A MapBounds object holds the bounds coordinates of :
     * <ul>
     * <li>The top-left corner of the map : (projectionX0, projectionY0) or (lon0, lat0) depending
     * on the map using a projection or not. </li>
     * <li>The bottom-right corner of the map : (projectionX1, projectionY1) or (lon1, lat1) </li>
     * </ul>
     */
    public static class MapBounds {
        static double DELTA = 0.0000001;
        public double X0;
        public double Y0;
        public double X1;
        public double Y1;

        public MapBounds(double x0, double y0, double x1, double y1) {
            X0 = x0;
            Y0 = y0;
            X1 = x1;
            Y1 = y1;
        }

        private static boolean doubleIsEqual(double d1, double d2, double delta) {
            if (Double.compare(d1, d2) == 0) {
                return true;
            }
            return (Math.abs(d1 - d2) <= delta);
        }

        public boolean compareTo(double x0, double y0, double x1, double y1) {
            return doubleIsEqual(X0, x0, DELTA) && doubleIsEqual(Y0, y0, DELTA) &&
                    doubleIsEqual(X1, x1, DELTA) && doubleIsEqual(Y1, y1, DELTA);
        }
    }
}
