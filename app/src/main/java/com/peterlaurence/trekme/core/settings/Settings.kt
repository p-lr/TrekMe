package com.peterlaurence.trekme.core.settings

import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File


/**
 * Holds global settings of TrekMe and exposes public methods to read/update those settings.
 * This class is thread safe (as its internal [FileSettingsHandler] is specifically designed
 * to have no shared mutable state).
 */
object Settings {
    private val settingsHandler = FileSettingsHandler()

    /**
     * Get the download directory as [File].
     * This implementation tries to get it from the configuration file. Since this file might have
     * been modified by an human, a check is done to fallback to the default download directory if
     * the path isn't among the list of possible paths.
     * It's also a security in the case the download directories change across versions.
     */
    suspend fun getDownloadDir(): File {
        return settingsHandler.getLastSetting().downloadDir.let {
            if (checkDownloadPath(it)) {
                File(it)
            } else {
                defaultDownloadDir
            }
        }
    }

    /**
     * Set the download directory.
     * This implementation first reads the current settings, creates a new instance of
     * [SettingsData], then then gives it to the [SettingsHandler] for write.
     */
    suspend fun setDownloadDir(file: File) {
        if (checkDownloadPath(file.absolutePath)) {
            val new = settingsHandler.getLastSetting().copy(downloadDir = file.absolutePath)
            settingsHandler.writeSetting(new)
        }
    }

    private fun checkDownloadPath(path: String): Boolean {
        return TrekMeContext.downloadDirList.map {
            it.absolutePath
        }.contains(path)
    }

    suspend fun getStartOnPolicy(): StartOnPolicy {
        return settingsHandler.getLastSetting().startOnPolicy
    }

    suspend fun setStartOnPolicy(policy: StartOnPolicy) {
        val new = settingsHandler.getLastSetting().copy(startOnPolicy = policy)
        settingsHandler.writeSetting(new)
    }

    suspend fun setMagnifyingFactor(factor: Int) {
        val new = settingsHandler.getLastSetting().copy(magnifyingFactor = factor)
        settingsHandler.writeSetting(new)
    }

    suspend fun getMagnifyingFactor(): Int = settingsHandler.getLastSetting().magnifyingFactor

    /**
     * @return The last map id, or null if it's undefined. The returned id is guarantied to be not
     * empty.
     */
    suspend fun getLastMapId(): Int? {
        val id = settingsHandler.getLastSetting().lastMapId
        return if (id != -1) {
            id
        } else {
            null
        }
    }

    /**
     * Set and saves the last map id, for further use.
     */
    suspend fun setLastMapId(id: Int) {
        val new = settingsHandler.getLastSetting().copy(lastMapId = id)
        settingsHandler.writeSetting(new)
    }
}

@Serializable
private data class SettingsData(val downloadDir: String = defaultDownloadDir.absolutePath,
                                val startOnPolicy: StartOnPolicy = StartOnPolicy.MAP_LIST,
                                val lastMapId: Int = -1,
                                val magnifyingFactor: Int = 1)

enum class StartOnPolicy {
    MAP_LIST, LAST_MAP
}


private val defaultDownloadDir = TrekMeContext.defaultMapsDownloadDir

private interface SettingsHandler {
    fun writeSetting(settingsData: SettingsData)
    suspend fun getLastSetting(): SettingsData
}

private class FileSettingsHandler : SettingsHandler {
    private val settingsFile = TrekMeContext.getSettingsFile()

    /* Channels */
    private val settingsToWrite = Channel<SettingsData>(Channel.CONFLATED)
    private val requests = Channel<Unit>(capacity = Channel.CONFLATED)
    private val lastSettings = Channel<SettingsData>(capacity = Channel.RENDEZVOUS)

    init {
        GlobalScope.worker(settingsToWrite, requests, lastSettings)
    }

    override fun writeSetting(settingsData: SettingsData) {
        settingsToWrite.offer(settingsData)
    }

    override suspend fun getLastSetting(): SettingsData {
        // offer a request while be don't get something back, to be sure to get something
        var settingsData: SettingsData?
        do {
            requests.offer(Unit)
            settingsData = lastSettings.poll()
        } while (settingsData == null)
        return settingsData
    }

    /**
     * The core coroutine that enables concurrent read/write of [SettingsData].
     * * [settingsToWrite] is the receive channel that is consumed to write into the config file
     * * [requests] is the receive channel that is consumed to update the conflated value of the
     * [lastSettings] channel.
     *
     * This way, the last value of [SettingsData] is stored in a thread-safe way.
     */
    private fun CoroutineScope.worker(settingsToWrite: ReceiveChannel<SettingsData>,
                                      requests: ReceiveChannel<Unit>,
                                      lastSettings: SendChannel<SettingsData>) {
        launch {
            var lastSetting = readSettingsOrDefault()
            lastSettings.send(lastSetting)

            while (true) {
                select<Unit> {
                    settingsToWrite.onReceive {
                        lastSetting = it
                        it.save()
                    }
                    requests.onReceive {
                        lastSettings.send(lastSetting)
                    }
                }
            }
        }
    }

    private fun readSettingsOrDefault(): SettingsData {
        return try {
            readSettings()
        } catch (e: Exception) {
            e.printStackTrace()
            /* In case of any error, return default settings */
            SettingsData()
        }
    }

    @UnstableDefault
    private fun readSettings(): SettingsData {
        // 1- get the settings file from TrekMeContext
        val file = settingsFile ?: throw Exception("No settings file")
        // 2- if it exists, read it
        if (file.exists()) {
            val json = FileUtils.getStringFromFile(file)
            /* This may throw Exceptions */
            return Json.parse(SettingsData.serializer(), json)
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
}