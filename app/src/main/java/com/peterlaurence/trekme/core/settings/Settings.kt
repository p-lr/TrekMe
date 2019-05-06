package com.peterlaurence.trekme.core.settings

import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File


object Settings {
    private val settingsFile = TrekMeContext.getSettingsFile()
    private val settingsData: SettingsData
        get() = readSettings()

    /**
     * Get the download directory as [File].
     * This implementation tries to get it from the configuration file. Since this file might have
     * been modified by an human, a check is done to fallback to the default download directory if
     * the path isn't among the list of possible paths.
     * It's also a security in the case the download directories change across versions.
     */
    fun getDownloadDir(): File {
        return try {
            val settings = readSettings()
            settings.downloadDir.let {
                if (checkDownloadPath(it)) {
                    File(it)
                } else {
                    defaultDownloadDir
                }
            }
        } catch (e: Exception) {
            defaultDownloadDir
        }
    }

    /**
     * Set the download directory.
     * This implementation first reads the current settings, creates a new instance of
     * [SettingsData], then writes it to the config file.
     *
     * @return whether or not save operation succeeded
     */
    fun setDownloadDir(file: File): Boolean {
        return if (checkDownloadPath(file.absolutePath)) {
            settingsData.copy(downloadDir = file.absolutePath).save()
        } else {
            false
        }
    }

    @UnstableDefault
    private fun readSettings(): SettingsData {
        // 1- get the settings file from TrekMeContext
        val file = settingsFile ?: throw Exception("No settings file")
        // 2- if it exists, read it
        if (file.exists()) {
            val json = FileUtils.getStringFromFile(file)
            return try {
                Json.parse(SettingsData.serializer(), json)
            } catch (e: Exception) {
                /* In case of any error, return default settings */
                SettingsData()
            }
        }
        throw Exception("Settings file path is wrong")
    }

    @UnstableDefault
    private fun SettingsData.save(): Boolean {
        return try {
            val st = Json.stringify(SettingsData.serializer(), this)
            FileUtils.writeToFile(st, settingsFile)
            true
        } catch (e: Exception) {
            false
        }

    }

    private fun checkDownloadPath(path: String): Boolean {
        return TrekMeContext.downloadDirList.map {
            it.absolutePath
        }.contains(path)
    }
}

@Serializable
private data class SettingsData(val downloadDir: String = defaultDownloadDir.absolutePath)

private val defaultDownloadDir = TrekMeContext.defaultMapsDownloadDir