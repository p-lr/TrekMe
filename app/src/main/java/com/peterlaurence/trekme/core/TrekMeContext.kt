package com.peterlaurence.trekme.core

import android.content.Context
import android.os.Environment
import android.util.Log

import java.io.File
import java.io.IOException

/**
 * General context attributes of the application.
 * Here are defined :
 *
 *  * The default root folder of the application on the external storage
 *  * Where maps are searched
 *  * Where maps can be downloaded
 *  * The default folder in which new maps downloaded from the internet are imported
 *  * The folder where credentials are stored
 *  * The folder where recordings are saved
 *  * The file in which app settings are saved (it's a private folder)
 *
 * @author peterLaurence on 07/10/17 -- converted to Kotlin on 20/11/18
 */
object TrekMeContext {
    const val appFolderName = "trekme"
    private const val appFolderNameLegacy = "trekadvisor"
    val defaultAppDir = File(Environment.getExternalStorageDirectory(),
            appFolderName)
    private val legacyAppDir = File(Environment.getExternalStorageDirectory(),
            appFolderNameLegacy)
    private val defaultMapsDir = defaultAppDir

    val defaultMapsDownloadDir = File(defaultMapsDir, "downloaded")
    val recordingsDir = File(defaultAppDir, "recordings")
    val credentialsDir = File(defaultAppDir, "credentials")

    /* Where maps are searched */
    var mapsDirList: List<File> = listOf(defaultAppDir)

    /* Where maps can be downloaded */
    val downloadDirList: List<File>
        get() = mapsDirList.map {
            File(it, "downloaded")
        }

    private var settingsFile: File? = null

    private const val TAG = "TrekMeContext"

    /**
     * Check whether the app root dir is in read-only state or not. This is usually used only if the
     * [checkAppDir] call returned `false`
     */
    val isAppDirReadOnly: Boolean
        get() = Environment.getExternalStorageState(defaultAppDir) == Environment.MEDIA_MOUNTED_READ_ONLY

    /**
     * Create necessary folders and files, and identify folder in which the maps will be searched
     * into.
     */
    fun init(context: Context) {
        try {
            createAppDirs()
            createNomediaFile()
            resolveMapDirs(context)
            createSettingsFile(context)
        } catch (e: SecurityException) {
            Log.e(TAG, "We don't have right access to create application folder")
        } catch (e: IOException) {
            Log.e(TAG, "We don't have right access to create application folder")
        }
    }

    /**
     * Get the settings [File], or null if for some reason it could not be created or this method
     * is called before its creation.
     */
    fun getSettingsFile(): File? {
        return if (settingsFile?.exists() != null) {
            settingsFile
        } else {
            null
        }
    }

    /**
     * The first [File] returned by [Context.getExternalFilesDirs] corresponds to an internal
     * storage which will be erased upon app uninstall. We don't want that so we strip this one and
     * keep [defaultAppDir] instead.
     *
     * Take at least the default app folder.
     */
    private fun resolveMapDirs(context: Context) {
        val dirs: List<File> = context.getExternalFilesDirs(null).filterIndexed { index, file ->
            index > 0 && file != null
        }

        mapsDirList = listOf(defaultAppDir) + dirs
    }

    /**
     * The settings file is stored in a private folder of the app, and this folder will be deleted
     * if the app is uninstalled. This is intended, not to persist those settings.
     */
    private fun createSettingsFile(context: Context) {
        settingsFile = File(context.filesDir, "settings.json")
        settingsFile?.also {
            if (!it.exists()) {
                it.createNewFile()
            }
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
        /* Root: try to import legacy first */
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
        if (legacyAppDir.exists()) {
            legacyAppDir.renameTo(defaultAppDir)
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
