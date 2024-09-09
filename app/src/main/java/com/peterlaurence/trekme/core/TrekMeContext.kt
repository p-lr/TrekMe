package com.peterlaurence.trekme.core

import android.content.Context
import android.os.Build.VERSION_CODES.Q
import android.os.Environment
import android.util.Log
import com.peterlaurence.trekme.core.map.data.CREDENTIALS_FOLDER_NAME
import com.peterlaurence.trekme.core.map.data.MAP_FOLDER_NAME
import com.peterlaurence.trekme.core.map.data.MAP_IMPORTED_FOLDER_NAME
import com.peterlaurence.trekme.core.map.data.RECORDINGS_FOLDER_NAME
import com.peterlaurence.trekme.util.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

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
 * @author P.Laurence on 07/10/17 -- converted to Kotlin on 20/11/18
 */
interface TrekMeContext {
    val defaultAppDir: StateFlow<File?>
    val defaultMapsDownloadDir: File?
    val importedDir: File?
    val recordingsDir: File?
    val recordingsDirFlow: StateFlow<File?>
    val rootDirListFlow: StateFlow<List<File>>
    val credentialsDir: File
    suspend fun isAppDirReadOnly(): Boolean
    suspend fun init(applicationContext: Context): Boolean
    suspend fun checkAppDir(): Boolean
}

class TrekMeContextAndroid : TrekMeContext {
    private val _rootDirListFlow = MutableStateFlow<List<File>>(emptyList())

    override val defaultAppDir = MutableStateFlow<File?>(null)
    override val rootDirListFlow: StateFlow<List<File>>
        get() = _rootDirListFlow

    override val defaultMapsDownloadDir: File? by lazy {
        defaultAppDir.value?.let {
            File(it, MAP_FOLDER_NAME)
        }
    }

    /* Where zip archives are extracted */
    override val importedDir: File? by lazy {
        defaultAppDir.value?.let {
            File(it, MAP_IMPORTED_FOLDER_NAME)
        }
    }

    override val recordingsDir: File? by lazy {
        defaultAppDir.value?.let {
            File(it, RECORDINGS_FOLDER_NAME)
        }
    }

    override val recordingsDirFlow: StateFlow<File?> = defaultAppDir.map {
        File(it, RECORDINGS_FOLDER_NAME)
    }

    private val TAG = "TrekMeContextAndroid"

    override val credentialsDir: File by lazy {
        File(defaultAppDir.value, CREDENTIALS_FOLDER_NAME)
    }

    /**
     * Check whether the app root dir is in read-only state or not. This is usually used only if the
     * [checkAppDir] call returned `false`
     */
    override suspend fun isAppDirReadOnly(): Boolean = withContext(Dispatchers.IO) {
        Environment.getExternalStorageState(defaultAppDir.value) == Environment.MEDIA_MOUNTED_READ_ONLY
    }

    /**
     * Create necessary folders and files, and identify folder in which the maps will be searched
     * into.
     * @param applicationContext The context that *should not* be an Activity context. It should be
     * obtained from [Context.getApplicationContext].
     */
    override suspend fun init(applicationContext: Context): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                resolveDirs(applicationContext)
                createAppDirs()
                true
            }.getOrDefault(false)
        }
    }

    /**
     * We distinguish two cases:
     * * Android < 10: We use the "trekme" folder in the internal memory as the default app dir.
     * Using [Context.getExternalFilesDirs], we indirectly use the directory on the SD card (if there
     * is one). The first [File] returned returned by that last api is a folder on the internal
     * memory whose files are removed when the app is uninstalled. This isn't the original behavior
     * of TrekMe so we don't use it on Android 9 and below.
     * * Android >= 10: We no longer use the "trekme" folder. Scoped storage imposes that the [File]
     * api can only be used within files returned by [Context.getExternalFilesDirs] - files that are
     * private to the app, either on the internal memory or on a SD card. So on Android 10 and above,
     * maps are deleted upon app uninstall. To circumvent this issue, the map save & restore
     * feature has been redesigned so that the user has more control on where maps are saved and
     * where they're restored from.
     */
    private fun resolveDirs(applicationContext: Context) {
        val dirs: List<File> = applicationContext.getExternalFilesDirs(null).filterNotNull()

        if (android.os.Build.VERSION.SDK_INT >= Q) {
            defaultAppDir.value = dirs.firstOrNull()
            _rootDirListFlow.value = dirs
        } else {
            defaultAppDir.value = File(Environment.getExternalStorageDirectory(), appFolderName)
            val otherDirs = dirs.drop(1)
            _rootDirListFlow.value = listOfNotNull(defaultAppDir.value) + otherDirs
        }
    }


    /**
     * To function properly, the app needs to have read + write access to its root directory
     */
    override suspend fun checkAppDir(): Boolean = withContext(Dispatchers.IO) {
        Environment.getExternalStorageState(defaultAppDir.value) == Environment.MEDIA_MOUNTED
    }

    @Throws(SecurityException::class)
    private fun createAppDirs() {
        /* Root: try to import legacy first */
        renameLegacyDir()
        createDir(defaultAppDir.value, "application")

        /* Credentials */
        createDir(credentialsDir, "credentials")

        /* Downloads */
        createDir(defaultMapsDownloadDir, "maps")

        /* Recordings */
        createDir(recordingsDir, "recordings")
    }

    /**
     * If we detect the existence of the legacy dir, rename it.
     * Only do this for Android version under 10, since the former default app dir was obtained with
     * a now deprecated call. People with Android 10 or new are very unlikely to have installed
     * TrekAdvisor anyway.
     */
    private fun renameLegacyDir() {
        if (android.os.Build.VERSION.SDK_INT < Q) {
            @Suppress("DEPRECATION")
            val legacyAppDir = File(Environment.getExternalStorageDirectory(),
                    appFolderNameLegacy)
            if (legacyAppDir.exists()) {
                val defaultAppDir = defaultAppDir
                defaultAppDir.value?.also {
                    legacyAppDir.renameTo(it)
                }
            }
        }
    }

    private fun createDir(dir: File?, label: String) {
        if (dir != null && !dir.exists()) {
            val created = dir.mkdir()
            if (!created) {
                Log.e(TAG, "Could not create $label folder")
            }
        }
    }
}

private const val appFolderName = "trekme"
private const val appFolderNameLegacy = "trekadvisor"
const val appName = "TrekMe"
