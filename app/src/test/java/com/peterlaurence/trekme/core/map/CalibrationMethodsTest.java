package com.peterlaurence.trekme.core.map;

import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethods;
import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint;
import com.peterlaurence.trekme.core.map.domain.models.MapBounds;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for calibration.
 *
 * @since 2017/11/19
 */
@RunWith(RobolectricTestRunner.class)
public class CalibrationMethodsTest {

    @Test
    public void calibration3Points() {
        CalibrationPoint pA = new CalibrationPoint(0.1, 0.1, 10, 10);

        CalibrationPoint pB = new CalibrationPoint(0.9, 0.1, 90, 10);

        CalibrationPoint pC = new CalibrationPoint(0.2, 0.9, 10, 90);

        MapBounds bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
        assertNotNull(bounds);
        assertTrue(bounds.compareTo(0, 0, 100, 100));
    }
}
