package com.peterlaurence.trekme.core.map


import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.data.dao.MapLoaderDaoFileBased
import com.peterlaurence.trekme.core.map.data.dao.MapSaverDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapSeekerDaoImpl
import com.peterlaurence.trekme.core.map.di.MapModule
import com.peterlaurence.trekme.core.map.domain.interactors.MapImportInteractor
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
 * @since 2016/08/19 -- Converted to Kotlin on 2019/10/27
 */
@RunWith(RobolectricTestRunner::class)
class MapImportInteractorTest {
    private val json = MapModule.provideJson()
    private val mapSaverDao = MapSaverDaoImpl(Dispatchers.Unconfined, Dispatchers.IO, json)
    private val mapLoaderDao = MapLoaderDaoFileBased(
        mapSaverDao, json, Dispatchers.IO
    )
    private val mapRepository = MapRepository()
    private val mapSeekerDao = MapSeekerDaoImpl(mapLoaderDao, mapSaverDao)

    private val mapImportInteractor = MapImportInteractor(mapRepository, mapSeekerDao)

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
                        val res = mapImportInteractor.importFromFile(libVipsMapDir)
                        val map = assertNotNull(res.map)

                        /* A subfolder under "libvips" subdirectory has been voluntarily created, to test
                         * the case when the import is done from a parent directory. Indeed, when a map is
                         * extracted from an archive, we don't know whether the map was zipped within a
                         * subdirectory or not. A way to know that is to analyse the extracted file structure.
                         */
                        assertEquals("mapname", map.name.value)

                        assertEquals(4, map.levelList.size.toLong())
                        assertEquals(256, map.levelList[0].tileSize.width.toLong())
                        assertEquals(".jpg", map.imageExtension)
                        assertEquals(true, File(expectedParentFolder, ".nomedia").exists())
                        assertNull(map.thumbnail.value)
                    } catch (e: Exception) {
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
                        val res = mapImportInteractor.importFromFile(libVipsMapDir)
                        val map = assertNotNull(res.map)
                        assertEquals("La Réunion - Est", map.name.value)
                        assertEquals(3, map.levelList.size.toLong())
                        val expectedParentFolder = File(libVipsMapDir, "reunion-est")
                        assertEquals(true, File(expectedParentFolder, ".nomedia").exists())
                    } catch (e: Exception) {
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
                val mapDirURL =
                    MapImportInteractorTest::class.java.classLoader!!.getResource("maps")
                mMapsDirectory = File(mapDirURL.toURI())
            } catch (e: Exception) {
                println("No resource file for map test directory.")
            }
        }
    }
}
