package com.peterlaurence.trekadvisor.core.geotools

import org.junit.Assert
import org.junit.Test

/**
 * @author peterLaurence on 30/03/18.
 */
class GeoToolsTest {
    @Test
    fun distanceApprox() {
        val d = distanceApprox(51.510357, -0.116773, 38.889931, -77.009003)
        /* Check the result with 10cm precision */
        Assert.assertEquals(d, 5897658.289, 0.1)
    }
}