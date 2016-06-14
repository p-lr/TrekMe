package com.peterlaurence.trekadvisor.core.map;

import android.support.annotation.Nullable;

import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

/**
 * The MapCalibrator provides different methods to obtain a {@link Map.MapBounds} from multiple
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
}
