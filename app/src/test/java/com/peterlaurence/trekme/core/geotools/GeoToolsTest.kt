package com.peterlaurence.trekme.core.geotools

import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test

/**
 * @author P.Laurence on 30/03/18.
 */
class GeoToolsTest {
    @Test
    fun distanceApproxTest() {
        val d = distanceApprox(51.510357, -0.116773, 38.889931, -77.009003)
        /* Check the result with 10cm precision */
        Assert.assertEquals(d, 5897658.289, 0.1)
    }

    @Test
    fun distanceApproxTest2() {
        val d = distanceApprox(48.722703, 2.510788, 48.868977, 2.310136)
        /* Check the result with 2m precision */
        Assert.assertEquals(d, 21920.0, 2.0)
    }

    @Test
    fun pointAtDistanceAndAngleTest() {
        val res1 = pointAtDistanceAndAngle(48.7221055, 2.5055106, 384260f, 269.17f)
        assertEquals(res1[0], 48.5536439, 0.00004)
        assertEquals(res1[1], -2.7187781, 0.00004)

        val res2 = pointAtDistanceAndAngle(16.218611, -61.745765, 413f, 118.06f)
        assertEquals(res2[0], 16.216855, 0.00001)
        assertEquals(res2[1], -61.742333, 0.00002)
    }
}