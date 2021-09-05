package com.peterlaurence.trekme.core.map


import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import java.io.File
import kotlin.test.*

/**
 * Unit tests for importing maps.
 *
 * @author P.Laurence on 19/08/16 -- Converted to Kotlin on 27/10/19
 */
@RunWith(RobolectricTestRunner::class)
class MapImporterTest {
    private val mapLoader = MapLoader(Dispatchers.Unconfined, Dispatchers.Default, Dispatchers.IO)

    @Before
    fun clear() {
        mapLoader.clearMaps()
    }

    @Test
    fun libvipsMapImporter() {
        if (mMapsDirectory != null) {
            val libVipsMapDir = File(mMapsDirectory, "libvips-no-json")
            val expectedParentFolder = File(libVipsMapDir, "mapname")
            if (libVipsMapDir.exists()) {
                /* Previous execution of this test created a map.json file and a .nomedia file So delete it. */
                val existingJsonFile = File(expectedParentFolder, MAP_FILENAME)
                if (existingJsonFile.exists()) {
                    existingJsonFile.delete()
                }
                val existingNomediaFile = File(expectedParentFolder, ".nomedia")
                if (existingNomediaFile.exists()) existingNomediaFile.delete()

                runBlocking {
                    try {
                        val res = MapImporter.importFromFile(libVipsMapDir, mapLoader)
                        val map = assertNotNull(res.map)

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. A way to know that is to analyse the extracted file structure.
                         */
                        assertEquals(expectedParentFolder, map.directory)
                        assertEquals("mapname", map.name)

                        assertEquals(4, map.configSnapshot.levels.size.toLong())
                        assertEquals(256, map.configSnapshot.levels[0].tileSize.width.toLong())
                        assertEquals(".jpg", map.imageExtension)
                        assertEquals(true, File(expectedParentFolder, ".nomedia").exists())
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
                        val res = MapImporter.importFromFile(libVipsMapDir, mapLoader)
                        val map = assertNotNull(res.map)
                        assertEquals("La Réunion - Est", map.name)
                        assertEquals(3, map.configSnapshot.levels.size.toLong())
                        val expectedParentFolder = File(libVipsMapDir, "reunion-est")
                        assertEquals(true, File(expectedParentFolder, ".nomedia").exists())
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
