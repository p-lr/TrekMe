package com.peterlaurence.trekme.core.map.domain.models;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The {@link CalibrationMethods} provides different methods to obtain a {@link MapBounds} from multiple
 * {@link CalibrationPoint} objects.
 *
 * @author P.Laurence
 */
public class CalibrationMethods {
    /**
     * This method calculates the {@link MapBounds} by extrapolating the projected values of
     * the two {@link CalibrationPoint} provided, so the returned {@link MapBounds} contains
     * the projected values for the exact top left and bottom right corners of the map.
     * <p/>
     * Note : Although the doc says that the calibration point A should be at top left corner and
     * the calibration point B at bottom right corner, this is not mandatory. But this is advised
     * to have the most xy-different calibration points (to get a precise calibration).
     *
     * @param calibrationPointA The calibration point at (approximately) top left corner
     * @param calibrationPointB The calibration point at (approximately) bottom right corner
     * @return The {@link MapBounds} object or null if calibration is not possible.
     */
    public static
    @Nullable
    MapBounds simple2PointsCalibration(CalibrationPoint calibrationPointA,
                                           CalibrationPoint calibrationPointB) {
        double delta_x = calibrationPointB.getNormalizedX() - calibrationPointA.getNormalizedX();
        double delta_y = calibrationPointB.getNormalizedY() - calibrationPointA.getNormalizedY();

        if (delta_x == 0 || delta_y == 0) {
            /* Incorrect calibration points */
            return null;
        }

        double delta_projectionX = calibrationPointB.getAbsoluteX() - calibrationPointA.getAbsoluteX();
        double delta_projectionY = calibrationPointB.getAbsoluteY() - calibrationPointA.getAbsoluteY();
        double projectionX0 = calibrationPointA.getAbsoluteX() - delta_projectionX / delta_x * calibrationPointA.getNormalizedX();
        double projectionY0 = calibrationPointA.getAbsoluteY() - delta_projectionY / delta_y * calibrationPointA.getNormalizedY();
        double projectionX1 = calibrationPointB.getAbsoluteX() + delta_projectionX / delta_x * (1 - calibrationPointB.getNormalizedX());
        double projectionY1 = calibrationPointB.getAbsoluteY() + delta_projectionY / delta_y * (1 - calibrationPointB.getNormalizedY());

        return new MapBounds(projectionX0, projectionY0, projectionX1, projectionY1);
    }

    /**
     * This calibration method uses three points. <br>
     * To determine the projected values, it uses the points that have the greatest extent in each
     * dimension. So the two points selected to compute de X values of the bounds may be different
     * from the ones used to compute de Y bounds.
     *
     * @return The {@link MapBounds} object or null if calibration is not possible.
     */
    public static
    @Nullable
    MapBounds calibrate3Points(CalibrationPoint calibrationPointA,
                                   CalibrationPoint calibrationPointB,
                                   CalibrationPoint calibrationPointC) {

        CalibrationPoint[] points = {calibrationPointA, calibrationPointB, calibrationPointC};

        /* Find the greatest difference in x */
        Arrays.sort(points, Comparator.comparingDouble(CalibrationPoint::getNormalizedX));
        double delta_x = points[2].getNormalizedX() - points[0].getNormalizedX();
        double delta_projectionX = points[2].getAbsoluteX() - points[0].getAbsoluteX();

        if (delta_x == 0) return null;

        double projectionX0 = points[0].getAbsoluteX() - delta_projectionX / delta_x * points[0].getNormalizedX();
        double projectionX1 = points[2].getAbsoluteX() + delta_projectionX / delta_x * (1 - points[2].getNormalizedX());

        /* Find the greatest difference in y */
        Arrays.sort(points, Comparator.comparingDouble(CalibrationPoint::getNormalizedY));
        double delta_y = points[2].getNormalizedY() - points[0].getNormalizedY();
        double delta_projectionY = points[2].getAbsoluteY() - points[0].getAbsoluteY();

        if (delta_y == 0) return null;

        double projectionY0 = points[0].getAbsoluteY() - delta_projectionY / delta_y * points[0].getNormalizedY();
        double projectionY1 = points[2].getAbsoluteY() + delta_projectionY / delta_y * (1 - points[2].getNormalizedY());

        return new MapBounds(projectionX0, projectionY0, projectionX1, projectionY1);
    }

    /**
     * This calibration method uses 4 points. <br>
     * It takes into account all provided points, but the more two points are distant, the more they
     * contribute to the value of the bounds.
     *
     * @return The {@link MapBounds} object or null if calibration is not possible.
     */
    public static
    @Nullable
    MapBounds calibrate4Points(CalibrationPoint calibrationPointA,
                                   CalibrationPoint calibrationPointB,
                                   CalibrationPoint calibrationPointC,
                                   CalibrationPoint calibrationPointD) {

        CalibrationPoint[] points = {calibrationPointA, calibrationPointB,
                calibrationPointC, calibrationPointD};

        /* Sort by ascending x */
        Arrays.sort(points, Comparator.comparingDouble(CalibrationPoint::getNormalizedX));
        double delta_x1 = points[3].getNormalizedX() - points[0].getNormalizedX();
        double delta_x2 = points[2].getNormalizedX() - points[0].getNormalizedX();
        double delta_x3 = points[1].getNormalizedX() - points[0].getNormalizedX();
        double delta_projectionX1 = points[3].getAbsoluteX() - points[0].getAbsoluteX();
        double delta_projectionX2 = points[2].getAbsoluteX() - points[0].getAbsoluteX();
        double delta_projectionX3 = points[1].getAbsoluteX() - points[0].getAbsoluteX();

        /* Barycentric medium */
        if (delta_x1 + delta_x2 + delta_x3 == 0) return null;
        double alpha_x = (delta_projectionX1 + delta_projectionX2 + delta_projectionX3) / (delta_x1 +
                delta_x2 + delta_x3);

        double projectionX0 = points[0].getAbsoluteX() - alpha_x * points[0].getNormalizedX();
        double projectionX1 = points[2].getAbsoluteX() + alpha_x * (1 - points[2].getNormalizedX());

        /* Sort by ascending y */
        Arrays.sort(points, Comparator.comparingDouble(CalibrationPoint::getNormalizedY));
        double delta_y1 = points[3].getNormalizedY() - points[0].getNormalizedY();
        double delta_y2 = points[2].getNormalizedY() - points[0].getNormalizedY();
        double delta_y3 = points[1].getNormalizedY() - points[0].getNormalizedY();
        double delta_projectionY1 = points[3].getAbsoluteY() - points[0].getAbsoluteY();
        double delta_projectionY2 = points[2].getAbsoluteY() - points[0].getAbsoluteY();
        double delta_projectionY3 = points[1].getAbsoluteY() - points[0].getAbsoluteY();

        /* Barycentric medium */
        if (delta_y1 + delta_y2 + delta_y3 == 0) return null;
        double alpha_y = (delta_projectionY1 + delta_projectionY2 + delta_projectionY3) / (delta_y1 +
                delta_y2 + delta_y3);

        double projectionY0 = points[0].getAbsoluteY() - alpha_y * points[0].getNormalizedY();
        double projectionY1 = points[2].getAbsoluteY() + alpha_y * (1 - points[2].getNormalizedY());

        return new MapBounds(projectionX0, projectionY0, projectionX1, projectionY1);
    }
}
