package com.peterlaurence.trekme.ui.mapcalibration;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import ovh.plrapps.mapview.MapView;
import ovh.plrapps.mapview.MapViewConfiguration;
import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.gson.MapGson;
import com.peterlaurence.trekme.core.projection.Projection;
import com.peterlaurence.trekme.repositories.map.MapRepository;
import com.peterlaurence.trekme.ui.mapcalibration.components.CalibrationMarker;
import com.peterlaurence.trekme.ui.tools.TouchMoveListener;
import com.peterlaurence.trekme.viewmodel.mapcalibration.MapCalibrationViewModel;

import java.lang.ref.WeakReference;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static ovh.plrapps.mapview.api.MarkerApiKt.addMarker;
import static ovh.plrapps.mapview.api.MarkerApiKt.moveMarker;
import static ovh.plrapps.mapview.api.MarkerApiKt.moveToMarker;
import static com.peterlaurence.trekme.viewmodel.common.tileviewcompat.CompatibityUtilsKt.makeMapViewTileStreamProvider;


/**
 * A {@link Fragment} subclass that allows the user to define calibration points of a map.
 * <p>
 * This {@code MapCalibrationFragment} is intended to be retained : it should not be re-created
 * on a configuration change.
 * </p>
 *
 * @author P.Laurence on 30/04/16.
 */
@AndroidEntryPoint
public class MapCalibrationFragment extends Fragment implements CalibrationModel {
    /* To restore the state upon configuration change */
    private static final String CALIBRATION_MARKER_X = "calibration_marker_x";
    private static final String CALIBRATION_MARKER_Y = "calibration_marker_y";
    private WeakReference<Map> mMapWeakReference;
    private MapCalibrationLayout rootView;
    private MapView mapView;
    private CalibrationMarker mCalibrationMarker;
    private int mCurrentCalibrationPoint;
    private View mView;
    MapCalibrationViewModel mViewModel;

    @Inject
    MapRepository mMapRepository;

    /**
     * Before telling the {@link MapView} to move a marker, we save its relative coordinates so we
     * can use them later on calibration save.
     */
    private static void moveCalibrationMarker(MapView mapView, View view, double x, double y) {
        CalibrationMarker calibrationMarker = (CalibrationMarker) view;
        calibrationMarker.setRelativeX(x);
        calibrationMarker.setRelativeY(y);
        moveMarker(mapView, view, x, y);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(MapCalibrationViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = new MapCalibrationLayout(getContext());
        rootView.setCalibrationModel(this);

        /* Set the map to calibrate */
        Map map = mMapRepository.getSettingsMap();
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
            moveCalibrationMarker(mapView, mCalibrationMarker, relativeX, relativeY);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mView = view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if (mCalibrationMarker != null) {
            savedInstanceState.putDouble(CALIBRATION_MARKER_X, mCalibrationMarker.getRelativeX());
            savedInstanceState.putDouble(CALIBRATION_MARKER_Y, mCalibrationMarker.getRelativeY());
        }
    }

    /**
     * Sets the map to generate a new {@link MapView}.
     *
     * @param map The new {@link Map} object
     */
    public void setMap(Map map) {
        /* Keep a weakRef for future references */
        mMapWeakReference = new WeakReference<>(map);

        MapView mapView = new MapView(this.getContext());

        int lvlCnt = map.getLevelList().size();
        int tileSize;
        if (lvlCnt > 0) {
            tileSize = map.getLevelList().get(0).tile_size.x;
        } else return;

        MapViewConfiguration config = new MapViewConfiguration(lvlCnt, map.getWidthPx(), map.getHeightPx(), tileSize,
                makeMapViewTileStreamProvider(map)).setMaxScale(2f);

        mapView.configure(config);

        /* Map calibration */
        mapView.defineBounds(0, 0, 1, 1);

        /* The calibration marker */
        mCalibrationMarker = new CalibrationMarker(this.getContext());
        TouchMoveListener.MarkerMoveAgent moveAgent = new CalibrationMarkerMarkerMoveAgent();
        mCalibrationMarker.setOnTouchListener(new TouchMoveListener(mapView, moveAgent));
        addMarker(mapView, mCalibrationMarker, 0.5, 0.5, -0.5f, -0.5f, 0f, 0f, null);

        /* Add the MapView to the root view */
        setMapView(mapView);

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
            moveCalibrationMarker(mapView, mCalibrationMarker, calibrationPoint.x, calibrationPoint.y);
        } else {
            /* No calibration point defined */
            moveCalibrationMarker(mapView, mCalibrationMarker, relativeX, relativeY);
        }
        moveToMarker(mapView, mCalibrationMarker, true);
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
        if (mViewModel != null) {
            mViewModel.saveMap(map);
        }

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

    private void setMapView(MapView mapView) {
        this.mapView = mapView;
        this.mapView.setId(R.id.tileview_calibration_id);
        this.mapView.setSaveEnabled(true);
        rootView.addView(this.mapView);
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

    private static class CalibrationMarkerMarkerMoveAgent implements TouchMoveListener.MarkerMoveAgent {
        @Override
        public void onMarkerMove(MapView mapView, View view, double x, double y) {
            moveCalibrationMarker(mapView, view, x, y);
        }
    }
}
