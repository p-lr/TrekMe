package com.peterlaurence.trekadvisor.core.mapsource.wmts

import org.junit.Assert
import org.junit.Test

class IgnTest {
    @Test
    fun tilesTest() {
        val p1 = Point(275951.78, 6241946.52)
        val p2 = Point(276951.78, 6240946.52)
        val tileIterable = getTileIterable(18, 18, p1, p2)

        val firstTile = tileIterable.first()
        Assert.assertEquals(132877, firstTile.col)
        Assert.assertEquals(90241, firstTile.row)

        val lastTile = tileIterable.last()
        Assert.assertEquals(132884, lastTile.col)
        Assert.assertEquals(90248, lastTile.row)
    }
}