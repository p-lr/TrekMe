package com.peterlaurence.trekme.core

import android.os.Environment
import android.util.Log

import java.io.File
import java.io.IOException

/**
 * General context attributes of the application. <br></br>
 * Here is defined :
 *
 *  * The root folder of the application on the external storage
 *  * Where maps are searched
 *  * The default folder in which new maps downloaded from the internet are imported
 *  * The folder where credentials are stored
 *  * The folder where recordings are saved
 *
 * @author peterLaurence on 07/10/17 -- converted to Kotlin on 20/11/18
 */
object TrekMeContext {
    val appFolderName = "trekme"
    private const val appFolderNameLegacy = "trekadvisor"
    val defaultAppDir = File(Environment.getExternalStorageDirectory(),
            appFolderName)
    private val legacyAppDir = File(Environment.getExternalStorageDirectory(),
            appFolderNameLegacy)
    /* For instance maps are searched anywhere under the app folder */
    val defaultMapsDir = defaultAppDir

    val defaultMapsDownloadDir = File(defaultMapsDir, "downloaded")
    val recordingsDir = File(defaultAppDir, "recordings")
    val credentialsDir = File(defaultAppDir, "credentials")
    private const val TAG = "TrekMeContext"

    /**
     * Check whether the app root dir is in read-only state or not. This is usually used only if the
     * [checkAppDir] call returned `false`
     */
    val isAppDirReadOnly: Boolean
        get() = Environment.getExternalStorageState(defaultAppDir) == Environment.MEDIA_MOUNTED_READ_ONLY

    /**
     * Create necessary folders and files.
     */
    fun init() {
        try {
            createAppDirs()
            createNomediaFile()
        } catch (e: SecurityException) {
            Log.e(TAG, "We don't have right access to create application folder")
        } catch (e: IOException) {
            Log.e(TAG, "We don't have right access to create application folder")
        }
    }

    /**
     * To function properly, the app needs to have read + write access to its root directory
     */
    fun checkAppDir(): Boolean {
        return Environment.getExternalStorageState(defaultAppDir) == Environment.MEDIA_MOUNTED
    }

    @Throws(SecurityException::class)
    private fun createAppDirs() {
        /* Root */
        renameLegacyDir()
        createDir(defaultAppDir, "application")

        /* Credentials */
        createDir(credentialsDir, "credentials")

        /* Downloads */
        createDir(defaultMapsDownloadDir, "downloads")

        /* Recordings */
        createDir(recordingsDir, "recordings")
    }

    /**
     * If we detect the existence of the legacy dir, rename it.
     */
    private fun renameLegacyDir() {
        try {
            if (legacyAppDir.exists()) {
                legacyAppDir.renameTo(defaultAppDir)
                println("renommage fai!")
            } else {
                println("renommage fait")
            }

        } catch (e: Exception) {
            println("renommage fail")
        }
    }

    private fun createDir(dir: File, label: String) {
        if (!dir.exists()) {
            val created = dir.mkdir()
            if (!created) {
                Log.e(TAG, "Could not create $label folder")
            }
        }
    }

    /**
     * We have to create an empty ".nomedia" file at the root of the application folder, so other
     * apps don't index this content for media files.
     */
    @Throws(SecurityException::class, IOException::class)
    private fun createNomediaFile() {
        if (defaultAppDir.exists()) {
            val noMedia = File(defaultAppDir, ".nomedia")
            val created = noMedia.createNewFile()
            if (!created) {
                Log.e(TAG, "Could not create .nomedia file")
            }
        }
    }
}
