package com.peterlaurence.trekme.core.map;

import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for calibration.
 *
 * @author P.Laurence on 19/11/17.
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

//        pC.setNormalizedX(0.95);
//        pC.setAbsoluteX(95);
//        bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
//        assertNotNull(bounds);
//        assertTrue(bounds.compareTo(0, 0, 100, 100));
//
//        pA.setNormalizedX(0.15);
//        pA.setAbsoluteY(5);
//        pB.setNormalizedY(0.95);
//        pB.setAbsoluteY(85);
//        bounds = CalibrationMethods.calibrate3Points(pA, pB, pC);
//        assertNotNull(bounds);
//        assertTrue(bounds.compareTo(-5.9375, -4.411764706, 100.3125, 89.70588235294119));
    }
}
