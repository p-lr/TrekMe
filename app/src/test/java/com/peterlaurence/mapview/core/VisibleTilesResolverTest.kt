package com.peterlaurence.mapview.core

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleTilesResolverTest {

    @Test
    fun levelTest() {
        val resolver = VisibleTilesResolver(8, 1000, 800)

        assertEquals(7, resolver.getCurrentLevel())
        resolver.setScale(0.7f)
        assertEquals(7, resolver.getCurrentLevel())
        resolver.setScale(0.5f)
        assertEquals(6, resolver.getCurrentLevel())
        resolver.setScale(0.26f)
        assertEquals(6, resolver.getCurrentLevel())
        resolver.setScale(0.15f)
        assertEquals(5, resolver.getCurrentLevel())
        resolver.setScale(0.0078f)
        assertEquals(0, resolver.getCurrentLevel())
        resolver.setScale(0.008f)
        assertEquals(1, resolver.getCurrentLevel())

        /* Outside of bounds test */
        resolver.setScale(0.0030f)
        assertEquals(0, resolver.getCurrentLevel())
        resolver.setScale(1f)
        assertEquals(7, resolver.getCurrentLevel())
    }
}