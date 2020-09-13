package com.peterlaurence.trekme.core.map

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoundingBoxTest {
    @Test
    fun intersectTest() {
        val box1 = BoundingBox(0.0, 2.0, 0.0, 2.0)
        val box2 = BoundingBox(1.0, 3.0, 1.0, 3.0)
        assertTrue(box1.intersects(box2))
        assertTrue(box2.intersects(box1))

        val box3 = BoundingBox(1.0, 1.5, 1.0, 1.5)
        assertTrue(box1.intersects(box3))
        assertTrue(box3.intersects(box1))

        val box4 = BoundingBox(-1.0, 1.5, -1.0, -0.5)
        assertFalse(box1.intersects(box4))
        assertFalse(box4.intersects(box1))

        val box5 = BoundingBox(-1.0, -0.5, 0.5, 1.5)
        assertFalse(box1.intersects(box5))
        assertFalse(box5.intersects(box1))

        val box6 = BoundingBox(-1.0, -0.5, 0.5, 3.0)
        assertFalse(box1.intersects(box6))
        assertFalse(box6.intersects(box1))
    }
}