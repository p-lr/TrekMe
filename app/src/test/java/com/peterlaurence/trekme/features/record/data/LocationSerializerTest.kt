package com.peterlaurence.trekme.features.record.data

import com.peterlaurence.trekme.features.record.data.datasource.LocationDeSerializerImpl
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LocationSerializerTest {

    /**
     * Tests the GPX decoding of a tmp file which is written during a recording.
     */
    @Test
    fun locationDeSerializerTest() = runTest {
        val tmpDirURL = LocationSerializerTest::class.java.classLoader!!.getResource("tmp")
        val tmpDir = File(tmpDirURL.toURI())
        val testFile = File(tmpDir, "recording-sample")

        val inputStream = FileInputStream(testFile)

        val locationSerializer = LocationDeSerializerImpl(inputStream)
        val gpx = locationSerializer.readGpx()

        assertNotNull(gpx)
        assertEquals(5, gpx.tracks.firstOrNull()?.trackSegments?.size)
        assertEquals(10, gpx.tracks.first().trackSegments.first().trackPoints.size)
        assertEquals(6, gpx.tracks.first().trackSegments.last().trackPoints.size)
    }
}