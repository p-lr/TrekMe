package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.core.map.maploader.MapLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Unit tests for maps's json file parsing.
 *
 * @author P.Laurence on 26/02/17 -- Converted to Kotlin on 17/02/2019
 */
@RunWith(RobolectricTestRunner::class)
class MapParserTest {
    @Test
    fun mapTracksParse() = runBlocking {
        if (jsonFilesDirectory != null) {
            val dirs = listOf(jsonFilesDirectory)
            val mapLoader = MapLoader
            mapLoader.clearMaps()
            val mapList = mapLoader.updateMaps(dirs.filterNotNull())

            /* One map should be found */
            assertEquals(1, mapList.size.toLong())
            val map = mapList[0]

            MapLoader.importRoutesForMap(map)
            assertEquals(2, map.routes!!.size.toLong())

            val route = map.routes!![0]
            assertEquals("A test route 1", route.name)
            assertTrue(route.visible)
            val markers = route.routeMarkers
            assertEquals(2, markers.size.toLong())

            val marker1 = markers[0]
            assertEquals("marker1", marker1.name)
            assertEquals(6198798.5047565, marker1.proj_x, 0.0)

            val marker2 = markers[1]
            assertEquals("marker2", marker2.name)
            assertEquals(-2418744.7142449305, marker2.proj_y, 0.0)
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
