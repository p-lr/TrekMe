package com.peterlaurence.trekme.util.gpx

import com.peterlaurence.trekme.util.gpx.model.ElevationSource
import com.peterlaurence.trekme.util.gpx.model.Gpx
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.util.*
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * GPX tests.
 *
 * @author P.Laurence on 12/02/17.
 */
@RunWith(RobolectricTestRunner::class)
class GPXTest {
    companion object {
        private var mGpxFilesDirectory: File? = null

        init {
            try {
                val gpxDirURL = GPXTest::class.java.classLoader!!.getResource("gpxfiles")
                mGpxFilesDirectory = File(gpxDirURL.toURI())
            } catch (e: Exception) {
                println("No resource file for gpx files directory.")
            }
        }
    }

    @Rule
    @JvmField
    var mTestFolder = TemporaryFolder()

    private val gpxSample = File(mGpxFilesDirectory, "sample_gpx_1.gpx")
    private val gpxRealRoute = File(mGpxFilesDirectory, "chauvigny_choucas.gpx")

    @Test
    fun simpleFileTest() = runBlocking {
        if (mGpxFilesDirectory != null) {
            if (gpxSample.exists()) {
                try {
                    val (metadata, trackList, wayPoints) = parseGpx(FileInputStream(gpxSample))

                    /* Metadata check */
                    assertNotNull(metadata)
                    assertEquals("Example gpx", metadata.name)
                    val cal = readTime(metadata.time)
                    assertEquals(2018, cal[Calendar.YEAR])
                    assertEquals(8, cal[Calendar.MONTH])
                    assertEquals(9, cal[Calendar.DAY_OF_MONTH])
                    assertEquals(47, cal[Calendar.SECOND])
                    assertEquals(2, trackList.size) // 1 track, 1 route
                    val eleSource = metadata.elevationSourceInfo
                    assertNotNull(eleSource)
                    assertEquals(ElevationSource.GPS, eleSource.elevationSource)
                    assertEquals(20, eleSource.sampling)

                    val (trackSegmentList, name, statistics) = trackList[0]
                    assertEquals("Example track", name)
                    assertEquals(1, trackSegmentList.size)
                    val (trackPointList) = trackSegmentList[0]
                    assertEquals(7, trackPointList.size)
                    val (lat, lon, elevation, time) = trackPointList[0]
                    assertEquals(46.57608333, lat)
                    assertEquals(8.89241667, lon)
                    assertEquals(2376.0, elevation)
                    assertEquals(getGpxDateParser().parse("2007-10-14T10:09:57Z")!!.time.toDouble(),
                            time!!.toDouble())

                    /* Check that the track has statistics */
                    assertNotNull(statistics)
                    assertEquals(statistics.distance, 102.0)
                    assertEquals(4, wayPoints.size)
                    val (latitude, longitude, elevation1, _, name1) = wayPoints[0]
                    assertEquals(54.9328621088893, latitude)
                    assertEquals(9.860624216140083, longitude)
                    assertEquals("Waypoint 1", name1)
                    assertEquals(127.1, elevation1)

                    /* Route tests */
                    val (trackSegments, name2) = trackList[1]
                    assertEquals("Patrick's Route", name2)
                    assertEquals(1, trackSegments.size)
                    // we only look after the first segment, as in our representation a route is
                    // a track with a single segment.
                    val (routePointList) = trackSegments[0]
                    assertEquals(4, routePointList.size)
                    val (latitude1, longitude1, elevation2) = routePointList[0]
                    assertEquals(54.9328621088893, latitude1)
                    assertEquals(9.860624216140083, longitude1)
                    assertEquals(141.7, elevation2)
                } catch (e: Exception) {
                    e.printStackTrace()
                    fail()
                }
            }
        }
    }

    /**
     * Tests the gpx writer against the gpx parser : parse an existing gpx file, use the gpx writer
     * to write a gpx file somewhere in a temp folder, then use the gpx parser again to parse the
     * resulting file.
     * The resulting file should have identical values (at least for tags that the writer supports).
     */
    @Test
    fun writeTest() = runBlocking {
        try {
            /* First read an existing gpx file */
            val gpxInput: Gpx?
            try {
                gpxInput = parseGpx(FileInputStream(gpxSample))
            } catch (e: Exception) {
                e.printStackTrace()
                fail()
            }

            /* Write it in a temporary folder */
            val testFile = mTestFolder.newFile()
            val fos = FileOutputStream(testFile)
            writeGpx(gpxInput, fos)

            /* Now read it back */
            val (metadata, trackList) = parseGpx(FileInputStream(testFile))

            /* Metadata check */
            assertNotNull(metadata)
            assertEquals("Example gpx", metadata.name)
            val cal = readTime(metadata.time)
            assertEquals(2018, cal[Calendar.YEAR])
            assertEquals(8, cal[Calendar.MONTH])
            assertEquals(9, cal[Calendar.DAY_OF_MONTH])
            assertEquals(47, cal[Calendar.SECOND])
            assertEquals(2, trackList.size)
            val eleSource = metadata.elevationSourceInfo
            assertNotNull(eleSource)
            assertEquals(ElevationSource.GPS, eleSource.elevationSource)
            assertEquals(20, eleSource.sampling)

            val (trackSegmentList, name, statistics) = trackList[0]
            assertEquals("Example track", name)
            assertEquals(1, trackSegmentList.size)
            val (trackPointList) = trackSegmentList[0]
            assertEquals(7, trackPointList.size)
            val (lat, lon, elevation, time) = trackPointList[0]
            assertEquals(46.57608333, lat)
            assertEquals(8.89241667, lon)
            assertEquals(2376.0, elevation)
            assertEquals(getGpxDateParser().parse("2007-10-14T10:09:57Z")!!.time.toDouble(), time!!.toDouble())
            assertNotNull(statistics)
            assertEquals(statistics.distance, 102.0)
        } catch (e: IOException) {
            e.printStackTrace()
            fail()
        } catch (e: ParserConfigurationException) {
            e.printStackTrace()
            fail()
        } catch (e: TransformerException) {
            e.printStackTrace()
            fail()
        } catch (e: ParseException) {
            e.printStackTrace()
            fail()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            fail()
        }
    }

    @Test
    fun readGpxWithRoute() = runBlocking {
        try {
            val (metadata, tracks) = parseGpx(FileInputStream(gpxRealRoute))
            assertNotNull(metadata)
            assertNull(metadata.name)
            assertEquals(1, tracks.size)
            assertEquals("Sentier Les Choucas- La Barre", tracks[0].name)
        } catch (e: Exception) {
            e.printStackTrace()
            fail()
        }
    }

    @Test
    fun dateParse() {
        val aDate = "2017-09-26T08:38:12+02:00"
        try {
            val date = getGpxDateParser().parse(aDate)
            assertNotNull(date)
            val cal = Calendar.getInstance()
            cal.time = date
            val year = cal[Calendar.YEAR]
            val month = cal[Calendar.MONTH]
            val day = cal[Calendar.DAY_OF_MONTH]
            val secs = cal[Calendar.SECOND]
            assertEquals(2017, year)
            assertEquals(8, month)
            assertEquals(26, day)
            assertEquals(12, secs)
        } catch (e: ParseException) {
            fail()
        }
    }

    private fun readTime(time: Long?): Calendar {
        val date = Date(time!!)
        val cal = Calendar.getInstance()
        cal.time = date
        return cal
    }
}