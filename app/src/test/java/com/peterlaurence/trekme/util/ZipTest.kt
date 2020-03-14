package com.peterlaurence.trekme.util

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import java.io.File
import java.io.IOException

import org.junit.Assert.fail
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * This tests the [ZipTask] against the [unzipTask].
 *
 * A sample map (a simple folder structure) located in the resources of the app is zipped in a
 * temporary folder. Right after that, it's unzipped in the same location. <br></br>
 * The test is considered successful if this operation is done completely without any error.
 *
 * @author peterLaurence on 10/08/17 -- Converted to Kotlin on 19/05/2019
 */
@RunWith(RobolectricTestRunner::class)
class ZipTest {

    private val mTestFolder = File(System.getProperty("java.io.tmpdir"), "junit_ziptest")

    @Test
    fun zipTest() {
        if (mMapsDirectory != null) {
            val libVipsMapDir = File(mMapsDirectory, MAP_NAME)

            val unzipProgressionListener = object : UnzipProgressionListener {
                override fun onProgress(p: Int) {

                }

                override fun onUnzipFinished(outputDirectory: File) {
                    println("Unzip finished")
                    FileUtils.deleteRecursive(mTestFolder)
                }

                override fun onUnzipError() {
                    fail()
                    FileUtils.deleteRecursive(mTestFolder)
                }
            }

            try {
                val tempMapArchive = File(mTestFolder, "testmap.zip")
                val size = tempMapArchive.length()
                tempMapArchive.parentFile.mkdirs()
                tempMapArchive.createNewFile()

                val progressionListener = object : ZipTask.ZipProgressionListener {
                    override fun fileListAcquired() {
                        println("File list acquired")
                    }

                    override fun onProgress(p: Int) {

                    }

                    override fun onZipFinished() {
                        unzipTask(FileInputStream(tempMapArchive), mTestFolder, size, unzipProgressionListener)
                    }

                    override fun onZipError() {
                        fail()
                    }
                }

                val zipTask = ZipTask(libVipsMapDir, FileOutputStream(tempMapArchive), progressionListener)
                zipTask.execute()

            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else {
            fail()
        }
    }

    companion object {
        private const val MAP_NAME = "libvips-with-json"
        private var mMapsDirectory: File? = null

        init {
            try {
                val mapDirURL = ZipTest::class.java.classLoader!!.getResource("maps")
                mMapsDirectory = File(mapDirURL.toURI())
            } catch (e: Exception) {
                println("No resource file for map test directory.")
            }
        }
    }
}
