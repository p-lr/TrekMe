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

    @Test
    fun viewportTest() {
        val resolver = VisibleTilesResolver(3, 1000, 800)
        var viewport = Viewport(0, 0,700, 512)

        var visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(2, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(2, visibleTiles.colRight)
        assertEquals(1, visibleTiles.rowBottom)

        resolver.setScale(0.5f)
        viewport = Viewport(0, 0, 200, 300)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(1, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(0, visibleTiles.colRight)
        assertEquals(1, visibleTiles.rowBottom)
    }
}