package com.peterlaurence.trekme.core.map


import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import java.io.File

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Unit tests for importing maps.
 *
 * @author peterLaurence on 19/08/16 -- Converted to Kotlin on 27/10/19
 */
@RunWith(RobolectricTestRunner::class)
class MapImporterTest {

    @Before
    fun clear() {
        MapLoader.clearMaps()
    }

    @Test
    fun libvipsMapImporter() {
        if (mMapsDirectory != null) {
            val libVipsMapDir = File(mMapsDirectory, "libvips-no-json")
            val expectedParentFolder = File(libVipsMapDir, "mapname")
            if (libVipsMapDir.exists()) {
                /* Previous execution of this test created a map.json file. So delete it. */
                val existingJsonFile = File(expectedParentFolder, MapLoader.MAP_FILE_NAME)
                if (existingJsonFile.exists()) {
                    existingJsonFile.delete()
                }

                runBlocking {
                    try {
                        val res = MapImporter.importFromFile(libVipsMapDir, Map.MapOrigin.VIPS)
                        val map = assertNotNull(res.map)

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. A way to know that is to analyse the extracted file structure.
                         */
                        assertEquals(expectedParentFolder, map.directory)
                        assertEquals("mapname", map.name)

                        assertEquals(4, map.mapGson.levels.size.toLong())
                        assertEquals(100, map.mapGson.levels[0].tile_size.x.toLong())
                        assertEquals(".jpg", map.imageExtension)
                        assertNull(map.image)
                    } catch (e: MapImporter.MapParseException) {
                        fail()
                    }
                }
            }
        }
    }

    @Test
    fun existingMapImport() {
        if (mMapsDirectory != null) {
            val libVipsMapDir = File(mMapsDirectory, "libvips-with-json")
            if (libVipsMapDir.exists()) {
                runBlocking {
                    try {
                        val res = MapImporter.importFromFile(libVipsMapDir, Map.MapOrigin.VIPS)
                        val map = assertNotNull(res.map)
                        assertEquals("La Réunion - Est", map.name)
                        assertEquals(3, map.mapGson.levels.size.toLong())
                    } catch (e: MapImporter.MapParseException) {
                        fail()
                    }
                }
            }
        }
    }

    companion object {
        private var mMapsDirectory: File? = null

        init {
            try {
                val mapDirURL = MapImporterTest::class.java.classLoader!!.getResource("maps")
                mMapsDirectory = File(mapDirURL.toURI())
            } catch (e: Exception) {
                println("No resource file for map test directory.")
            }
        }
    }
}
