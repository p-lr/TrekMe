package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.domain.logic.getGeoStatistics
import com.peterlaurence.trekme.core.lib.gpx.parseGpx
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class TrackToolsTest {
    private val gpxDir by lazy {
        val gpxDirURL = TrackToolsTest::class.java.classLoader!!.getResource("gpxfiles")
        File(gpxDirURL.toURI())
    }

    @Test
    fun `test distance calculation of multi-segment track`() = runBlocking {
        val gpxFile = File(gpxDir, "sceaux.gpx")
        assertTrue { gpxFile.exists() }

        val gpx = parseGpx(FileInputStream(gpxFile))
        assertEquals(1, gpx.tracks.size)
        val geoRecord = gpxToDomain(gpx, gpxFile.name)
        val stats = getGeoStatistics(geoRecord)
        assertEquals(3773.11, stats.distance, 0.01)
    }

    @Test
    fun `test bounding box calculation`() = runBlocking {
        val gpxFile = File(gpxDir, "Randopitons_1136.gpx")
        assertTrue { gpxFile.exists() }

        val gpx = parseGpx(FileInputStream(gpxFile))
        assertEquals(1, gpx.tracks.size)
        val geoRecord = gpxToDomain(gpx, gpxFile.name)
        val stats = getGeoStatistics(geoRecord)
        val bb = stats.boundingBox
        assertNotNull(bb)
        assertEquals(-21.19779156, bb.maxLat, 0.00001)
        assertEquals(55.632350504, bb.maxLon, 0.00001)
        assertEquals(-21.246825328, bb.minLat, 0.00001)
        assertEquals(55.614207426, bb.minLon, 0.00001)
    }
}