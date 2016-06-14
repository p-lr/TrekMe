package com.peterlaurence.trekadvisor.menu.mapcalibration;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapLoader;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;
import com.peterlaurence.trekadvisor.core.projection.Projection;
import com.peterlaurence.trekadvisor.menu.mapcalibration.components.CalibrationMarker;
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
    private WeakReference<Map> mMapWeakReference;

    private MapCalibrationLayout rootView;
    private TileView mTileView;
    private CalibrationMarker mCalibrationMarker;
    private List<MapGson.Calibration.CalibrationPoint> mCalibrationPointList;
    private int mCurrentCalibrationPoint;

    /*
     * The interface that the view associated with this fragment must implement.
     */
    public interface MapCalibrationView {
        void updateCoordinateFields(double x, double y);

        void setCalibrationModel(CalibrationModel l);

        void setup();

        void noProjectionDefined();

        void projectionDefined();

        boolean isWgs84();

        double getXValue();

        double getYValue();
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @return A new instance of {@code MapSettingsFragment}
     */
    public static MapCalibrationFragment newInstance() {
        return new MapCalibrationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null && rootView != null) {
            return rootView;
        }

        rootView = new MapCalibrationLayout(getContext());
        rootView.setCalibrationModel(this);
        return rootView;
    }

    /**
     * Sets the map to generate a new {@link TileView}.
     *
     * @param map The new {@link Map} object
     */
    public void setMap(Map map) {
        /* Keep a weakRef for future references */
        mMapWeakReference = new WeakReference<>(map);

        /* Get the calibration points */
        mCalibrationPointList = map.getCalibrationPoints();

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
        mCalibrationMarker.setOnTouchListener(new MarkerTouchMoveListener(tileView));
        tileView.addMarker(mCalibrationMarker, 0.5, 0.5, -0.5f, -0.5f);

        /* The BitmapProvider */
        tileView.setBitmapProvider(map.getBitmapProvider());

        /* Remove the existing TileView, then add the new one */
        removeCurrentTileView();
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
        moveToCalibrationPoint(0, 0.9, 0.1);
        mCurrentCalibrationPoint = 2;
    }

    @Override
    public void onFourthCalibrationPointSelected() {
        updateCoordinateFieldsFromData(3);
        moveToCalibrationPoint(0, 0.1, 0.9);
        mCurrentCalibrationPoint = 3;
    }

    @Override
    public void onWgs84modeChanged(boolean isWgs84) {
        double x = rootView.getXValue();
        double y = rootView.getYValue();
        Projection projection = mMapWeakReference.get().getProjection();
        if (isWgs84) {
            projection.undoProjection(x, y);
            double[] wgs84 = projection.getWgs84Coords();
            rootView.updateCoordinateFields(wgs84[1], wgs84[0]);
        } else {
            projection.doProjection(y, x);
            double[] projectedValues = projection.getProjectedValues();
            rootView.updateCoordinateFields(projectedValues[0], projectedValues[1]);
        }
    }

    private void moveToCalibrationPoint(int calibrationPointNumber, double relativeX, double relativeY) {
        if (mCalibrationPointList.size() > calibrationPointNumber) {
            MapGson.Calibration.CalibrationPoint calibrationPoint = mCalibrationPointList.get(calibrationPointNumber);
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

        Map map = mMapWeakReference.get();
        MapGson.Calibration.CalibrationPoint calibrationPoint = mCalibrationPointList.get(mCurrentCalibrationPoint);
        Projection projection = map.getProjection();
        if (rootView.isWgs84() && projection != null) {
            projection.doProjection(y, x);
            double[] projectedValues = projection.getProjectedValues();
            calibrationPoint.proj_x = projectedValues[0];
            calibrationPoint.proj_y = projectedValues[1];
        } else {
            calibrationPoint.proj_x = x;
            calibrationPoint.proj_y = y;
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
        if (mCalibrationPointList.size() > calibrationPointNumber) {
            MapGson.Calibration.CalibrationPoint calibrationPoint = mCalibrationPointList.get(calibrationPointNumber);
            Projection projection = mMapWeakReference.get().getProjection();
            if (rootView.isWgs84() && projection != null) {
                projection.undoProjection(calibrationPoint.proj_x, calibrationPoint.proj_y);
                double[] wgs84 = projection.getWgs84Coords();
                if (wgs84.length == 2) {
                    rootView.updateCoordinateFields(wgs84[1], wgs84[0]);
                }
            } else {
                rootView.updateCoordinateFields(calibrationPoint.proj_x, calibrationPoint.proj_y);
            }
        }
    }

    /**
     * A touch listener that enables touch-moves of a marker.
     * Example of usage :
     * <pre>{@code
     * MarkerTouchMoveListener markerTouchListener = new MarkerTouchMoveListener(tileView);
     * mPositionMarker = new PositionMarker(context);
     * mPositionMarker.setOnTouchListener(markerTouchListener);
     * }</pre>
     */
    private static class MarkerTouchMoveListener implements View.OnTouchListener {

        private final TileView mTileView;
        private double deltaX;
        private double deltaY;

        public MarkerTouchMoveListener(TileView tileView) {
            mTileView = tileView;
        }

        @Override
        public boolean onTouch(final View view, MotionEvent event) {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    deltaX = getRelativeX(view.getX() - view.getWidth() / 2 + event.getX()) - getRelativeX(view.getX());
                    deltaY = getRelativeY(view.getY() - view.getHeight() / 2 + event.getY()) - getRelativeY(view.getY());
                    break;

                case MotionEvent.ACTION_MOVE:
                    double X = getRelativeX(view.getX() + event.getX());
                    double Y = getRelativeY(view.getY() + event.getY());
                    moveCalibrationMarker(mTileView, view, X - deltaX, Y - deltaY);
                    break;

                default:
                    return false;
            }
            return true;
        }

        private double getRelativeX(float x) {
            return mTileView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        }

        private double getRelativeY(float y) {
            return mTileView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());
        }
    }

    private void removeCurrentTileView() {
        try {
            mTileView.destroy();
            rootView.removeView(mTileView);
        } catch (Exception e) {
            // don't care
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
        Toast toast = Toast.makeText(this.getContext(), saveOkMsg, Toast.LENGTH_SHORT);
        toast.show();
    }

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
}
