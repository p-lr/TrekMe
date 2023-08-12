package com.peterlaurence.trekme.core.mapsource.wmts

import com.peterlaurence.trekme.core.wmts.domain.model.Point
import com.peterlaurence.trekme.core.wmts.domain.tools.getMapSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class IgnTest {
    @Test
    fun oneLevelTest() {
        val p1 = Point(275951.78, 6241946.52)
        val p2 = Point(276951.78, 6240946.52)
        val tileSequence = getMapSpec(18, 18, p1, p2, tileSize = 256).tileSequence

        val firstTile = tileSequence.first()
        assertEquals(132877, firstTile.col)
        assertEquals(90241, firstTile.row)

        val lastTile = tileSequence.last()
        assertEquals(132884, lastTile.col)
        assertEquals(90248, lastTile.row)
    }

    @Test
    fun severalLevelsTest() {
        val p1 = Point(250597.29, 6238768.52)
        val p2 = Point(306729.05, 6187019.47)

        val tileSequence = getMapSpec(12, 16, p1, p2, tileSize = 256).tileSequence
        val byLevel = tileSequence.groupBy {
            it.level
        }

        assertEquals(1410, byLevel[12]?.firstOrNull()?.row)
        assertEquals(2073, byLevel[12]?.firstOrNull()?.col)

        assertEquals(2820, byLevel[13]?.firstOrNull()?.row)
        assertEquals(4146, byLevel[13]?.firstOrNull()?.col)

        assertEquals(5640, byLevel[14]?.firstOrNull()?.row)
        assertEquals(8292, byLevel[14]?.firstOrNull()?.col)

        assertEquals(11280, byLevel[15]?.firstOrNull()?.row)
        assertEquals(16584, byLevel[15]?.firstOrNull()?.col)

        assertEquals(22560, byLevel[16]?.firstOrNull()?.row)
        assertEquals(33168, byLevel[16]?.firstOrNull()?.col)
    }

    @Test
    fun mapSizeTest() {
        val p1 = Point(250597.29, 6238768.52)
        val p2 = Point(306729.05, 6187019.47)

        val spec = getMapSpec(12, 16, p1, p2, tileSize = 256)

        assertEquals(26368, spec.mapWidthPx)
        assertEquals(23552, spec.mapHeightPx)
    }
}