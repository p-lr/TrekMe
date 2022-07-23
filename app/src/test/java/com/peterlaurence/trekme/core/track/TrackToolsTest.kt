package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.georecord.data.convertGpx
import com.peterlaurence.trekme.core.lib.gpx.parseGpx
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
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
        val geoRecord = convertGpx(gpx)
        val stats = TrackTools.getGeoStatistics(geoRecord)
        assertEquals(3773.11, stats.distance, 0.01)
    }
}