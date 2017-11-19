package com.peterlaurence.trekadvisor.core.map;

import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

import java.util.Arrays;

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
     * @return The {@link Map.MapBounds} object or null if calibration is not possible.
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
     * This calibration method uses three points. <br>
     * To determine the projected values, it uses the points that have the greatest extent in each
     * dimension. So the two points selected to compute de X values of the bounds may be different
     * from the ones used to compute de Y bounds.
     *
     * @param calibrationPointA One calibration point
     * @param calibrationPointB One calibration point
     * @param calibrationPointC One calibration point
     * @return The {@link Map.MapBounds} object or null if calibration is not possible.
     */
    public static
    @Nullable
    Map.MapBounds calibrate3Points(MapGson.Calibration.CalibrationPoint calibrationPointA,
                                   MapGson.Calibration.CalibrationPoint calibrationPointB,
                                   MapGson.Calibration.CalibrationPoint calibrationPointC) {

        MapGson.Calibration.CalibrationPoint[] points = {calibrationPointA, calibrationPointB, calibrationPointC};

        /* Find the greatest difference in x */
        Arrays.sort(points, (p1, p2) -> p1.x > p2.x ? 1 : -1);
        double delta_x = points[2].x - points[0].x;
        double delta_projectionX = points[2].proj_x - points[0].proj_x;

        if (delta_x == 0) return null;

        double projectionX0 = points[0].proj_x - delta_projectionX / delta_x * points[0].x;
        double projectionX1 = points[2].proj_x + delta_projectionX / delta_x * (1 - points[2].x);

        /* Find the greatest difference in y */
        Arrays.sort(points, (p1, p2) -> p1.y > p2.y ? 1 : -1);
        double delta_y = points[2].y - points[0].y;
        double delta_projectionY = points[2].proj_y - points[0].proj_y;

        if (delta_y == 0) return null;

        double projectionY0 = points[0].proj_y - delta_projectionY / delta_y * points[0].y;
        double projectionY1 = points[2].proj_y + delta_projectionY / delta_y * (1 - points[2].y);

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
