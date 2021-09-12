package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.repositories.map.RouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertNotNull

/**
 * Unit tests for maps's json file parsing.
 *
 * @author P.Laurence on 26/02/17 -- Converted to Kotlin on 17/02/2019
 */
@RunWith(RobolectricTestRunner::class)
class MapParserTest {
    private val mapLoader = MapLoader(Dispatchers.Unconfined, Dispatchers.Default, Dispatchers.IO)
    private val routeRepository = RouteRepository(Dispatchers.IO, Dispatchers.Unconfined)

    @Test
    fun mapTracksParse() = runBlocking {
        if (jsonFilesDirectory != null) {
            val dirs = listOf(jsonFilesDirectory)
            mapLoader.clearMaps()
            val mapList = mapLoader.updateMaps(dirs.filterNotNull())

            /* One map should be found */
            assertEquals(1, mapList.size.toLong())
            val map = mapList[0]

            routeRepository.importRoutes(map)
            assertEquals(2, map.routes!!.size.toLong())

            val route = map.routes!![0]
            assertEquals("A test route 1", route.name)
            assertTrue(route.visible)
            val markers = route.routeMarkers
            assertEquals(2, markers.size.toLong())

            val marker1 = markers[0]
            assertEquals("marker1", marker1.name)
            val projX = marker1.proj_x
            assertNotNull(projX)
            assertEquals(6198798.5047565, projX, 0.0)

            val marker2 = markers[1]
            assertEquals("marker2", marker2.name)
            val projY = marker2.proj_y
            assertNotNull(projY)
            assertEquals(-2418744.7142449305, projY, 0.0)
        }
    }

    companion object {
        private var jsonFilesDirectory: File? = null

        init {
            try {
                val mapDirURL = MapImporterTest::class.java.classLoader!!.getResource("mapjson-example")
                jsonFilesDirectory = File(mapDirURL.toURI())
            } catch (e: Exception) {
                println("No json directory found.")
            }

        }
    }
}
