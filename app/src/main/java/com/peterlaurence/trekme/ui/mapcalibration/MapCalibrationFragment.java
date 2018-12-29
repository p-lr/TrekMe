package com.peterlaurence.trekme.ui.mapcalibration;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MapGson;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.core.projection.Projection;
import com.peterlaurence.trekme.ui.mapcalibration.components.CalibrationMarker;
import com.peterlaurence.trekme.ui.tools.MarkerTouchMoveListener;
import com.peterlaurence.trekme.model.map.MapProvider;
import com.qozix.tileview.TileView;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * A {@link Fragment} subclass that allows the user to define calibration points of a map.
 * <p>
 * This {@code MapCalibrationFragment} is intended to be retained : it should not be re-created
 * on a configuration change.
 * </p>
 *
 * @author peterLaurence on 30/04/16.
 */
public class MapCalibrationFragment extends Fragment implements CalibrationModel {
    /* To restore the state upon configuration change */
    private static final String CALIBRATION_MARKER_X = "calibration_marker_x";
    private static final String CALIBRATION_MARKER_Y = "calibration_marker_y";
    private WeakReference<Map> mMapWeakReference;
    private MapCalibrationLayout rootView;
    private TileView mTileView;
    private CalibrationMarker mCalibrationMarker;
    private int mCurrentCalibrationPoint;
    private View mView;

    /**
     * Before telling the {@link TileView} to move a marker, we save its relative coordinates so we
     * can use them later on calibration save.
     */
    private static void moveCalibrationMarker(TileView tileView, View view, double x, double y) {
        CalibrationMarker calibrationMarker = (CalibrationMarker) view;
        calibrationMarker.setRelativeX(x);
        calibrationMarker.setRelativeY(y);
        tileView.moveMarker(view, x, y);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = new MapCalibrationLayout(getContext());
        rootView.setCalibrationModel(this);

        /* Set the map to calibrate */
        Map map = MapProvider.INSTANCE.getSettingsMap();
        setMap(map);

        /* If the fragment is created for the first time (e.g not re-created after a configuration
         * change), init the layout to its default.
         * Otherwise, restore the last position of the calibration marker.
         */
        if (savedInstanceState == null) {
            rootView.setDefault();
        } else {
            double relativeX = savedInstanceState.getDouble(CALIBRATION_MARKER_X);
            double relativeY = savedInstanceState.getDouble(CALIBRATION_MARKER_Y);
            moveCalibrationMarker(mTileView, mCalibrationMarker, relativeX, relativeY);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mView = view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mCalibrationMarker != null) {
            savedInstanceState.putDouble(CALIBRATION_MARKER_X, mCalibrationMarker.getRelativeX());
            savedInstanceState.putDouble(CALIBRATION_MARKER_Y, mCalibrationMarker.getRelativeY());
        }
    }

    /**
     * Sets the map to generate a new {@link TileView}.
     *
     * @param map The new {@link Map} object
     */
    public void setMap(Map map) {
        /* Keep a weakRef for future references */
        mMapWeakReference = new WeakReference<>(map);

        TileView tileView = new TileView(this.getContext());

        /* Set the size of the view in px at scale 1 */
        tileView.setSize(map.getWidthPx(), map.getHeightPx());

        /* Lowest scale */
        List<MapGson.Level> levelList = map.getLevelList();
        float scale = 1 / (float) Math.pow(2, levelList.size() - 1);

        /* Scale limits */
        tileView.setScaleLimits(scale, 2);

        /* Starting scale */
        tileView.setScale(scale);

        /* DetailLevel definition */
        for (MapGson.Level level : levelList) {
            tileView.addDetailLevel(scale, level.level, level.tile_size.x, level.tile_size.y);
            /* Calculate each level scale for best precision */
            scale = 1 / (float) Math.pow(2, levelList.size() - level.level - 2);
        }

        /* Panning outside of the map is not possible --affects minimum scale */
        tileView.setShouldScaleToFit(true);

        /* Disable animations. As of 03/2016, it leads to performance drops */
        tileView.setTransitionsEnabled(false);

        /* Render while panning */
        tileView.setShouldRenderWhilePanning(true);

        /* Map calibration */
        tileView.defineBounds(0, 0, 1, 1);

        /* The calibration marker */
        mCalibrationMarker = new CalibrationMarker(this.getContext());
        MarkerTouchMoveListener.MarkerMoveCallback callback = new CalibrationMarkerMoveCallback();
        mCalibrationMarker.setOnTouchListener(new MarkerTouchMoveListener(tileView, callback));
        tileView.addMarker(mCalibrationMarker, 0.5, 0.5, -0.5f, -0.5f);

        /* The BitmapProvider */
        tileView.setBitmapProvider(map.getBitmapProvider());

        /* Add the TileView to the root view */
        setTileView(tileView);

        /* Update the ui */
        rootView.setup();

        /* Check whether the Map has defined a projection */
        if (map.getProjection() == null) {
            rootView.noProjectionDefined();
        } else {
            rootView.projectionDefined();
        }
    }

    @Override
    public void onFirstCalibrationPointSelected() {
        updateCoordinateFieldsFromData(0);
        moveToCalibrationPoint(0, 0.1, 0.1);
        mCurrentCalibrationPoint = 0;
    }

    @Override
    public void onSecondCalibrationPointSelected() {
        updateCoordinateFieldsFromData(1);
        moveToCalibrationPoint(1, 0.9, 0.9);
        mCurrentCalibrationPoint = 1;
    }

    @Override
    public void onThirdCalibrationPointSelected() {
        updateCoordinateFieldsFromData(2);
        moveToCalibrationPoint(2, 0.9, 0.1);
        mCurrentCalibrationPoint = 2;
    }

    @Override
    public void onFourthCalibrationPointSelected() {
        updateCoordinateFieldsFromData(3);
        moveToCalibrationPoint(3, 0.1, 0.9);
        mCurrentCalibrationPoint = 3;
    }

    @Override
    public void onWgs84modeChanged(boolean isWgs84) {
        Projection projection = mMapWeakReference.get().getProjection();
        if (projection == null) return;

        double x = rootView.getXValue();
        double y = rootView.getYValue();

        if (isWgs84) {
            double[] wgs84 = projection.undoProjection(x, y);
            if (wgs84 != null) {
                rootView.updateCoordinateFields(wgs84[0], wgs84[1]);
            }
        } else {
            double[] projectedValues = projection.doProjection(y, x);
            if (projectedValues != null) {
                rootView.updateCoordinateFields(projectedValues[0], projectedValues[1]);
            }
        }
    }

    private void moveToCalibrationPoint(int calibrationPointNumber, double relativeX, double relativeY) {
        /* Get the calibration points */
        Map map = mMapWeakReference.get();
        if (map == null) return;
        List<MapGson.Calibration.CalibrationPoint> calibrationPointList = map.getCalibrationPoints();

        if (calibrationPointList != null && calibrationPointList.size() > calibrationPointNumber) {
            MapGson.Calibration.CalibrationPoint calibrationPoint = calibrationPointList.get(calibrationPointNumber);
            moveCalibrationMarker(mTileView, mCalibrationMarker, calibrationPoint.x, calibrationPoint.y);
        } else {
            /* No calibration point defined */
            moveCalibrationMarker(mTileView, mCalibrationMarker, relativeX, relativeY);
        }
        mTileView.moveToMarker(mCalibrationMarker, true);
    }

    @Override
    public int getCalibrationPointNumber() {
        return mMapWeakReference.get().getCalibrationPointsNumber();
    }

    /**
     * Save the current calibration point. Its index is saved in {@code mCurrentCalibrationPoint}.
     */
    @Override
    public void onSave() {
        double x = rootView.getXValue();
        double y = rootView.getYValue();

        if (x == Double.MAX_VALUE || y == Double.MAX_VALUE) {
            return;
        }

        /* Get the calibration points */
        Map map = mMapWeakReference.get();
        if (map == null) return;
        List<MapGson.Calibration.CalibrationPoint> calibrationPointList = map.getCalibrationPoints();

        MapGson.Calibration.CalibrationPoint calibrationPoint;
        if (calibrationPointList.size() > mCurrentCalibrationPoint) {
            calibrationPoint = calibrationPointList.get(mCurrentCalibrationPoint);
        } else {
            calibrationPoint = new MapGson.Calibration.CalibrationPoint();
            map.addCalibrationPoint(calibrationPoint);
        }
        Projection projection = map.getProjection();
        if (rootView.isWgs84() && projection != null) {
            double[] projectedValues = projection.doProjection(y, x);
            if (projectedValues != null) {
                calibrationPoint.proj_x = projectedValues[0];
                calibrationPoint.proj_y = projectedValues[1];
            } else {
                displayErrorMessage(R.string.projected_instead_of_wgs84);
                return;
            }
        } else {
            /* If no projection is defined or no mistake is detected, we continue */
            if (projection == null || projection.undoProjection(x, y) != null) {
                calibrationPoint.proj_x = x;
                calibrationPoint.proj_y = y;
            } else {
                /* ..else, show error message and stop */
                displayErrorMessage(R.string.wgs84_instead_of_projected);
                return;
            }
        }

        /* Save relative position */
        calibrationPoint.x = mCalibrationMarker.getRelativeX();
        calibrationPoint.y = mCalibrationMarker.getRelativeY();

        /* Update calibration */
        map.calibrate();

        /* Save */
        MapLoader.getInstance().saveMap(map);

        showSaveConfirmation();
    }

    private void updateCoordinateFieldsFromData(int calibrationPointNumber) {
        /* Get the calibration points */
        Map map = mMapWeakReference.get();
        if (map == null) return;
        List<MapGson.Calibration.CalibrationPoint> calibrationPointList = map.getCalibrationPoints();

        if (calibrationPointList != null && calibrationPointList.size() > calibrationPointNumber) {
            MapGson.Calibration.CalibrationPoint calibrationPoint = calibrationPointList.get(calibrationPointNumber);
            Projection projection = mMapWeakReference.get().getProjection();
            if (rootView.isWgs84() && projection != null) {
                double[] wgs84 = projection.undoProjection(calibrationPoint.proj_x, calibrationPoint.proj_y);
                if (wgs84 != null && wgs84.length == 2) {
                    rootView.updateCoordinateFields(wgs84[0], wgs84[1]);
                }
            } else {
                rootView.updateCoordinateFields(calibrationPoint.proj_x, calibrationPoint.proj_y);
            }
        }
    }

    private void setTileView(TileView tileView) {
        mTileView = tileView;
        mTileView.setId(R.id.tileview_calibration_id);
        mTileView.setSaveEnabled(true);
        rootView.addView(mTileView);
    }

    private void showSaveConfirmation() {
        String saveOkMsg = getString(R.string.calibration_point_saved);
        Snackbar snackbar = Snackbar.make(mView, saveOkMsg, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private void displayErrorMessage(int stringId) {
        Snackbar snackbar = Snackbar.make(rootView, stringId, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    /*
     * The interface that the view associated with this fragment must implement.
     */
    interface MapCalibrationView {
        void updateCoordinateFields(double x, double y);

        void setCalibrationModel(CalibrationModel l);

        void setup();

        /* Called only when the view is created for the first time */
        void setDefault();

        void noProjectionDefined();

        void projectionDefined();

        boolean isWgs84();

        double getXValue();

        double getYValue();
    }

    private static class CalibrationMarkerMoveCallback implements MarkerTouchMoveListener.MarkerMoveCallback {
        @Override
        public void onMarkerMove(TileView tileView, View view, double x, double y) {
            moveCalibrationMarker(tileView, view, x, y);
        }
    }
}
