package com.peterlaurence.trekme.core.map;

import com.peterlaurence.trekme.BuildConfig;
import com.peterlaurence.trekme.core.map.gson.MapGson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for calibration.
 *
 * @author peterLaurence on 19/11/17.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class CalibrationMethodsTest {

    @Test
    public void calibration3Points() {
        MapGson.Calibration.CalibrationPoint pA = new MapGson.Calibration.CalibrationPoint();
        pA.x = 0.1;
        pA.y = 0.1;
        pA.proj_x = 10;
        pA.proj_y = 10;

        MapGson.Calibration.CalibrationPoint pB = new MapGson.Calibration.CalibrationPoint();
        pB.x = 0.9;
        pB.y = 0.1;
        pB.proj_x = 90;
        pB.proj_y = 10;

        MapGson.Calibration.CalibrationPoint pC = new MapGson.Calibration.CalibrationPoint();
        pC.x = 0.2;
        pC.y = 0.9;
        pC.proj_x = 10;
        pC.proj_y = 90;

        Map.MapBounds bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
        assertNotNull(bounds);
        assertTrue(bounds.compareTo(0, 0, 100, 100));

        pC.x = 0.95;
        pC.proj_x = 95;
        bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
        assertNotNull(bounds);
        assertTrue(bounds.compareTo(0, 0, 100, 100));

        pA.x = 0.15;
        pA.proj_y = 5;
        pB.y = 0.95;
        pB.proj_y = 85;
        bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
        assertNotNull(bounds);
        assertTrue(bounds.compareTo(-5.9375, -4.411764706, 100.3125, 89.70588235294119));
    }
}
