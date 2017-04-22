package com.peterlaurence.trekadvisor.core.map;

import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

/**
 * The {@link MapCalibrator} provides different methods to obtain a {@link Map.MapBounds} from multiple
 * {@link MapGson.Calibration.CalibrationPoint} objects.
 *
 * @author peterLaurence
 */
public class MapCalibrator {
    /**
     * This method calculates the {@link Map.MapBounds} by extrapolating the projected values of
     * the two {@link MapGson.Calibration.CalibrationPoint} provided, so the returned {@link Map.MapBounds} contains
     * the projected values for the exact top left and bottom right corners of the map.
     * <p/>
     * Note : Although the doc says that the calibration point A should be at top left corner and
     * the calibration point B at bottom right corner, this is not mandatory. But this is advised
     * to have the most xy-different calibration points (to get a precise calibration).
     *
     * @param calibrationPointA The calibration point at (approximately) top left corner
     * @param calibrationPointB The calibration point at (approximately) bottom right corner
     * @return The {@link Map.MapBounds} object
     */
    public static
    @Nullable
    Map.MapBounds simple2PointsCalibration(MapGson.Calibration.CalibrationPoint calibrationPointA,
                                           MapGson.Calibration.CalibrationPoint calibrationPointB) {
        double delta_x = calibrationPointB.x - calibrationPointA.x;
        double delta_y = calibrationPointB.y - calibrationPointA.y;

        if (delta_x == 0 || delta_y == 0) {
            /* Incorrect calibration points */
            return null;
        }

        double delta_projectionX = calibrationPointB.proj_x - calibrationPointA.proj_x;
        double delta_projectionY = calibrationPointB.proj_y - calibrationPointA.proj_y;
        double projectionX0 = calibrationPointA.proj_x - delta_projectionX / delta_x * calibrationPointA.x;
        double projectionY0 = calibrationPointA.proj_y - delta_projectionY / delta_y * calibrationPointA.y;
        double projectionX1 = calibrationPointB.proj_x + delta_projectionX / delta_x * (1 - calibrationPointB.x);
        double projectionY1 = calibrationPointB.proj_y + delta_projectionY / delta_y * (1 - calibrationPointB.y);

        return new Map.MapBounds(projectionX0, projectionY0, projectionX1, projectionY1);
    }

    /**
     * x and y values are expected to be between 0 and 1 : they respectively represent the position
     * in percent of the calibration point relatively to the map width and height. <br>
     * This method can change the {@link MapGson.Calibration.CalibrationPoint}.
     */
    static void sanityCheck2PointsCalibration(MapGson.Calibration.CalibrationPoint calibrationPointA,
                                              MapGson.Calibration.CalibrationPoint calibrationPointB) {
        /* Check whether one of the two points is apparently ok */
        boolean okA = checkPercentPositionBounds(calibrationPointA);
        boolean okB = checkPercentPositionBounds(calibrationPointB);
        boolean different = checkPercentPositionDifferent(calibrationPointA, calibrationPointB);

        if (!different || (!okA && !okB)) {
            init2Points(calibrationPointA, calibrationPointB);
        } else if (okA && !okB) {
            reposition2PointCalibration(calibrationPointB, calibrationPointA);
        } else if (!okA) {
            reposition2PointCalibration(calibrationPointA, calibrationPointB);
        }
    }

    private static boolean checkPercentPositionBounds(MapGson.Calibration.CalibrationPoint calibrationPoint) {
        return !(calibrationPoint.x < 0 || calibrationPoint.x > 1 || calibrationPoint.y < 0 ||
                calibrationPoint.y > 1);
    }

    private static boolean checkPercentPositionDifferent(MapGson.Calibration.CalibrationPoint calibrationPointA,
                                                         MapGson.Calibration.CalibrationPoint calibrationPointB) {
        return calibrationPointA.x != calibrationPointB.x && calibrationPointA.y != calibrationPointB.y;
    }

    private static void init2Points(MapGson.Calibration.CalibrationPoint calibrationPointA,
                                    MapGson.Calibration.CalibrationPoint calibrationPointB) {
        calibrationPointA.x = 0;
        calibrationPointA.y = 0;
        calibrationPointB.x = 1;
        calibrationPointB.y = 1;
    }

    private static void reposition2PointCalibration(MapGson.Calibration.CalibrationPoint calibrationPointWrong,
                                                    MapGson.Calibration.CalibrationPoint calibrationPointRef) {
        calibrationPointWrong.x = 1 - calibrationPointRef.x;
        calibrationPointWrong.y = 1 - calibrationPointRef.y;
    }
}
