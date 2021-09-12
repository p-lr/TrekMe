package com.peterlaurence.trekme.core.projection;

import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author P.Laurence on 18/02/18.
 */
public class MercatorProjectionTest {
    private final Projection mProjection = new MercatorProjection();

    @Test
    public void centerOfParis() {
        double[] projValues = mProjection.doProjection(48.853554, 2.352102);
        assertNotNull(projValues);
        assertEquals(projValues[0], 261834.84, 0.1);
        assertEquals(projValues[1], 6250049.02, 0.1);

        double[] wgs84Values = mProjection.undoProjection(261834.84, 6250049.02);
        assertNotNull(wgs84Values);
        assertEquals(wgs84Values[0], 2.352102, 0.000001);
        assertEquals(wgs84Values[1], 48.853554, 0.000001);

        /* Test that erroneous values return null */
        double[] projValuesErr = mProjection.doProjection(261834.84, 6250049.02);
        assertNull(projValuesErr);
    }
}
