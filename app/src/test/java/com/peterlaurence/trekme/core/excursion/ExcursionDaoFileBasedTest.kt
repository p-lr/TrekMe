package com.peterlaurence.trekme.core.excursion

import com.peterlaurence.trekme.core.excursion.data.dao.ExcursionDaoFileBased
import com.peterlaurence.trekme.core.excursion.data.model.Waypoint
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExcursionDaoFileBasedTest {
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    private val testScope = TestScope(testDispatcher)

    private val dao = ExcursionDaoFileBased(
        listOfNotNull(excursionDir),
        appDirFlow = flowOf(),
        geoRecordParser = { file ->
            parseGpxSafely(file.inputStream())?.let {
                gpxToDomain(it, file.name)
            }
        },
        ioDispatcher = testDispatcher
    )

    @Test
    fun excursionParseTest() = testScope.runTest {
        val excursions = dao.getExcursionsFlow()

        assertEquals(1, excursions.value.size)
        val excursion = excursions.value.first()
        assertEquals("Excursion 1", excursion.title)
        assertEquals("This the description for excursion 1", excursion.description)
        assertEquals(2, excursion.photos.size)
        assertEquals("photo_id_2", excursion.photos.last().id)
    }

    @Test
    fun waypointParseTest() = testScope.runTest {
        val excursions = dao.getExcursionsFlow()

        assertEquals(1, excursions.value.size)
        val excursion = excursions.value.first()
        val waypoints = dao.getWaypoints(excursion)
        assertEquals(2, waypoints.size)
        assertEquals("p1", waypoints.first().photos.first().id)
        assertEquals("wpt1 photo1", waypoints.first().photos.first().name)
        assertEquals("p1.jpg", (waypoints.first() as Waypoint).photos.first().fileName)
    }

    @Test
    fun geoRecordDetectionTest() = testScope.runTest {
        val excursions = dao.getExcursionsFlow()

        assertEquals(1, excursions.value.size)
        val excursion = excursions.value.first()
        val geoRecord = dao.getGeoRecord(excursion)
        assertEquals("sceaux.gpx", geoRecord?.name)
    }

    companion object {
        private val excursionDir: File? = try {
            val url = ExcursionDaoFileBasedTest::class.java.classLoader!!.getResource("excursions")
            File(url.toURI()).parentFile
        } catch (e: Exception) {
            println("Error while getting excursions test dir")
            null
        }
    }
}