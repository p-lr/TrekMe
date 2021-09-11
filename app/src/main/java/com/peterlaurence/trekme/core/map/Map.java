package com.peterlaurence.trekme.core.map;

import static com.peterlaurence.trekme.core.map.ConstantsKt.MAP_FILENAME;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peterlaurence.trekme.core.map.domain.Calibration;
import com.peterlaurence.trekme.core.map.domain.CalibrationMethod;
import com.peterlaurence.trekme.core.map.domain.CalibrationPoint;
import com.peterlaurence.trekme.core.map.domain.Landmark;
import com.peterlaurence.trekme.core.map.domain.Level;
import com.peterlaurence.trekme.core.map.domain.MapConfig;
import com.peterlaurence.trekme.core.map.domain.MapOrigin;
import com.peterlaurence.trekme.core.map.domain.Marker;
import com.peterlaurence.trekme.core.map.domain.Route;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    /* The Java Object corresponding to the json configuration file */
    private final MapConfig mMapConfig;

    private List<Landmark> mLandmarks;
    private List<Marker> mMarkerList;
    private List<Route> mRouteList;

    private CalibrationStatus mCalibrationStatus = CalibrationStatus.NONE;

    /**
     * To create a {@link Map}, three parameters are needed. <br>
     *
     * @param mapConfig the {@link MapConfig} object that includes informations relative to levels,
     *                  the tile size, the name of the map, etc.
     * @param jsonFile  the {@link File} for serialization.
     * @param thumbnail the {@link File} image for map customization.
     */
    public Map(MapConfig mapConfig, File jsonFile, File thumbnail) {
        mMapConfig = mapConfig;
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
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            Projection proj = cal.getProjection();
            if (proj != null) return proj.getName();
        }
        return null;
    }

    public
    @Nullable
    Projection getProjection() {
        try {
            return mMapConfig.getCalibration().getProjection();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public void setProjection(Projection projection) {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            Calibration newCal = cal.copy(projection, cal.getCalibrationMethod(), cal.getCalibrationPoints());
            mMapConfig.setCalibration(newCal);
        }
    }

    /**
     * Get the bounds the map. See {@link MapBounds}.
     */
    public
    @Nullable
    MapBounds getMapBounds() {
        return mMapBounds;
    }

    public
    @Nullable
    Long getSizeInBytes() {
        return mMapConfig.getSizeInBytes();
    }

    public void setSizeInBytes(@NonNull Long size) {
        mMapConfig.setSizeInBytes(size);
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
        Calibration cal = mMapConfig.getCalibration();
        if (cal == null) return;

        /* Init the projection */
        Projection projection = cal.getProjection();
        if (projection != null) {
            projection.init();
        }

        List<CalibrationPoint> calibrationPoints = cal.getCalibrationPoints();
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
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null && cal.getCalibrationPoints().size() >= 2) {
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
        return mMapConfig.getName();
    }

    public void setName(String newName) {
        mMapConfig.setName(newName);
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
     * Markers are lazily loaded.
     */
    @Nullable
    public List<Marker> getMarkers() {
        return mMarkerList;
    }

    public void setMarkers(List<Marker> markers) {
        mMarkerList = markers;
    }

    public void addMarker(Marker marker) {
        mMarkerList.add(marker);
    }

    @Nullable
    public final List<Landmark> getLandmarks() {
        return mLandmarks;
    }

    public void setLandmarks(List<Landmark> landmarks) {
        mLandmarks = landmarks;
    }

    public void addLandmark(Landmark landmark) {
        mLandmarks.add(landmark);
    }

    public void deleteLandmark(Landmark landmark) {
        mLandmarks.remove(landmark);
    }

    public boolean areLandmarksDefined() {
        return mLandmarks != null && mLandmarks.size() > 0;
    }

    /**
     * Routes are lazily loaded.
     */
    @Nullable
    public List<Route> getRoutes() {
        return mRouteList;
    }

    public void setRoutes(List<Route> routes) {
        mRouteList = routes;
    }

    /**
     * Add a new route to the map.
     */
    public void addRoute(Route route) {
        if (mRouteList != null) {
            mRouteList.add(route);
        }
    }

    public void replaceRoute(@NonNull Route from, @NonNull Route to) {
        int i = mRouteList.indexOf(from);
        if (i != -1) {
            mRouteList.remove(i);
            mRouteList.add(i, to);
        } else {
            mRouteList.add(to);
        }
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
        mMapConfig.setThumbnail(THUMBNAIL_NAME);
    }

    public List<Level> getLevelList() {
        return mMapConfig.getLevels();
    }

    @Nullable
    public CalibrationMethod getCalibrationMethod() {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) return cal.getCalibrationMethod();
        return null;
    }

    public void setCalibrationMethod(CalibrationMethod method) {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            Calibration newCal = cal.copy(cal.getProjection(), method, cal.getCalibrationPoints());
            mMapConfig.setCalibration(newCal);
        }
    }

    @Nullable
    public MapOrigin getOrigin() {
        try {
            return mMapConfig.getOrigin();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Get the number of calibration that should be defined.
     */
    public int getCalibrationPointsNumber() {
        CalibrationMethod method = getCalibrationMethod();
        if (method == null) return 0;

        switch (method) {
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
    public List<CalibrationPoint> getCalibrationPoints() {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            return cal.getCalibrationPoints();
        } else {
            return Collections.emptyList();
        }
    }

    public void addCalibrationPoint(CalibrationPoint point) {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            List<CalibrationPoint> newPoints = new ArrayList<>(cal.getCalibrationPoints());
            newPoints.add(point);
            Calibration newCal = cal.copy(cal.getProjection(), cal.getCalibrationMethod(),
                    newPoints);
            mMapConfig.setCalibration(newCal);
        }
    }

    public void setCalibrationPoints(List<CalibrationPoint> points) {
        Calibration cal = mMapConfig.getCalibration();
        if (cal != null) {
            Calibration newCal = cal.copy(cal.getProjection(), cal.getCalibrationMethod(), points);
            mMapConfig.setCalibration(newCal);
        }
    }

    public String getImageExtension() {
        return mMapConfig.getImageExtension();
    }

    public int getWidthPx() {
        return mMapConfig.getSize().getWidth();
    }

    public int getHeightPx() {
        return mMapConfig.getSize().getHeight();
    }

    public final MapConfig getConfigSnapshot() {
        // TODO: make a defensive copy
        return mMapConfig;
    }

    public final File getConfigFile() {
        return mConfigFile;
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
