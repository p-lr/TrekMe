package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.BuildConfig
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getRoutesForMap
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.io.File
import java.net.URL

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Unit tests for maps's json file parsing.
 *
 * @author peterLaurence on 26/02/17 -- Converted to Kotlin on 17/02/2019
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MapParserTest {

    @Test
    fun mapTracksParse() = runBlocking {
        if (mJsonFilesDirectory != null) {
            val dirs = listOf(mJsonFilesDirectory)
            val map = arrayOfNulls<Map>(1)

            fun routeChecks(map: Map) {
                getRoutesForMap(map).invokeOnCompletion {
                    assertEquals(2, map.routes!!.size.toLong())

                    val route = map.routes!![0]
                    assertEquals("A test route 1", route.name)
                    assertTrue(route.visible)
                    val markers = route.route_markers
                    assertEquals(2, markers.size.toLong())

                    val marker1 = markers[0]
                    assertEquals("marker1", marker1.name)
                    assertEquals(6198798.5047565, marker1.proj_x, 0.0)

                    val marker2 = markers[1]
                    assertEquals("marker2", marker2.name)
                    assertEquals(-2418744.7142449305, marker2.proj_y, 0.0)
                }
            }

            val mapListUpdateListener = object : MapLoader.MapListUpdateListener {
                override fun onMapListUpdate(mapsFound: Boolean) {
                    val mapList = MapLoader.maps

                    /* One map should be found */
                    assertEquals(1, mapList.size.toLong())
                    map[0] = mapList[0]

                    routeChecks(map[0]!!)
                }
            }

            val mapLoader = MapLoader
            mapLoader.setMapListUpdateListener(mapListUpdateListener)
            mapLoader.clearAndGenerateMaps(dirs.filterNotNull())
        }
    }

    companion object {
        private var mJsonFilesDirectory: File? = null

        init {
            try {
                val mapDirURL = MapImporterTest::class.java.classLoader!!.getResource("mapjson-example")
                mJsonFilesDirectory = File(mapDirURL.toURI())
            } catch (e: Exception) {
                println("No json directory found.")
            }

        }
    }
}
