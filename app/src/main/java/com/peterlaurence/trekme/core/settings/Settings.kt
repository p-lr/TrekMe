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
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Holds global settings of TrekMe and exposes public methods to read/update those settings.
 * This class is thread safe (as its internal [FileSettingsHandler] is specifically designed
 * to have no shared mutable state).
 */
@Singleton
class Settings @Inject constructor(private val trekMeContext: TrekMeContext) {
    private val settingsHandler: FileSettingsHandler by lazy {
        FileSettingsHandler(trekMeContext)
    }

    /**
     * Get the current application directory as [File].
     * This implementation tries to get it from the configuration file. Since this file might have
     * been modified by an human, a check is done to fallback to the default application directory
     * if the path isn't among the list of possible paths.
     * It's also a security in the case the application directories change across versions.
     */
    suspend fun getAppDir(): File? {
        return settingsHandler.getLastSetting().appDir.let {
            if (checkAppPath(it)) {
                File(it)
            } else {
                trekMeContext.defaultAppDir
            }
        }
    }

    /**
     * Set the application directory.
     * This implementation first reads the current settings, creates a new instance of
     * [SettingsData], then then gives it to the [SettingsHandler] for write.
     */
    suspend fun setAppDir(file: File) {
        if (checkAppPath(file.absolutePath)) {
            val new = settingsHandler.getLastSetting().copy(appDir = file.absolutePath)
            settingsHandler.writeSetting(new)
        }
    }

    private fun checkAppPath(path: String): Boolean {
        return trekMeContext.mapsDirList?.map {
            it.absolutePath
        }?.contains(path) ?: false
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

    suspend fun getRotationMode(): RotationMode {
        return settingsHandler.getLastSetting().rotationMode
    }

    suspend fun setRotationMode(mode: RotationMode) {
        val new = settingsHandler.getLastSetting().copy(rotationMode = mode)
        settingsHandler.writeSetting(new)
    }

    suspend fun setDefineScaleCentered(defined: Boolean) {
        val new = settingsHandler.getLastSetting().copy(defineScaleWhenCenter = defined)
        settingsHandler.writeSetting(new)
    }

    suspend fun getDefineScaleCentered(): Boolean {
        return settingsHandler.getLastSetting().defineScaleWhenCenter
    }

    suspend fun setScaleCentered(scaleCentered: Float) {
        val new = settingsHandler.getLastSetting().copy(scaleCentered = scaleCentered)
        settingsHandler.writeSetting(new)
    }

    suspend fun getScaleCentered(): Float {
        return settingsHandler.getLastSetting().scaleCentered
    }

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

/**
 * @param appDir The current application directory
 * @param startOnPolicy Whether TrekMe should boot on the map list or on the last map
 * @param defineScaleWhenCenter If `true`, [scaleCentered] is accounted for. Otherwise,
 * [scaleCentered] is ignored.
 * @param scaleCentered The scale at which the MapView is set when centering on the current position
 */
@Serializable
private data class SettingsData(val appDir: String,
                                val startOnPolicy: StartOnPolicy = StartOnPolicy.MAP_LIST,
                                val lastMapId: Int = -1,
                                val magnifyingFactor: Int = 0,
                                val rotationMode: RotationMode = RotationMode.NONE,
                                val defineScaleWhenCenter: Boolean = true,
                                val scaleCentered: Float = 1f)

enum class StartOnPolicy {
    MAP_LIST, LAST_MAP
}

enum class RotationMode {
    NONE, FOLLOW_ORIENTATION, FREE
}


private interface SettingsHandler {
    suspend fun writeSetting(settingsData: SettingsData)
    suspend fun getLastSetting(): SettingsData
}

private class FileSettingsHandler(private val trekMeContext: TrekMeContext) : SettingsHandler {
    private val settingsFile = trekMeContext.getSettingsFile()

    /* Channels */
    private val settingsToWrite = Channel<SettingsData>()
    private val requests = Channel<Unit>(capacity = 1)
    private val lastSettings = Channel<SettingsData>()

    init {
        GlobalScope.actor(settingsToWrite, requests, lastSettings)
    }

    override suspend fun writeSetting(settingsData: SettingsData) {
        settingsToWrite.send(settingsData)
    }

    /**
     * The internal [requests] channel having a capacity of 1, the order in which this method
     * returns a [SettingsData] instance is preserved. For example, if two consumers call
     * [getLastSetting] at approximately the same time, the first one which adds an element to
     * [requests] is guaranteed to receive a [SettingsData] instance before the other consumer which
     * is then suspended trying to send an element to [requests].
     */
    override suspend fun getLastSetting(): SettingsData {
        requests.send(Unit)
        return lastSettings.receive()
    }

    /**
     * The core coroutine that enables concurrent read/write of [SettingsData].
     * * [settingsToWrite] is the receive channel that is consumed to write into the config file
     * * [requests] is the receive channel that is consumed to update the last value of the
     * [lastSettings] channel.
     *
     * This way, the last value of [SettingsData] is stored in a thread-safe way.
     */
    private fun CoroutineScope.actor(settingsToWrite: ReceiveChannel<SettingsData>,
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
            SettingsData(trekMeContext.defaultAppDir?.absolutePath ?: "error")
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