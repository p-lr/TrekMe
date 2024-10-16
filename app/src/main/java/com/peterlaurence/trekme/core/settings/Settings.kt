package com.peterlaurence.trekme.core.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordExportFormat
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerInfo
import com.peterlaurence.trekme.core.units.DistanceUnit
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.util.android.safeData
import com.peterlaurence.trekme.util.android.safeEdit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Holds global settings of TrekMe and exposes public methods to read/update those settings.
 * This class internally uses the Jetpack DataStore. Getters expose [Flow]s while setters are
 * suspending functions. When a [Flow] is being collected, the collector will receive the new value
 * if we invoke the corresponding setter.
 *
 * N.B: It's important that this class (which encapsulates the DataStore) acts as a singleton - as
 * multiple instances of DataStore using the same file name would break.
 */
@Singleton
class Settings @Inject constructor(
    private val trekMeContext: TrekMeContext,
    private val app: Application
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS)

    private val dataStore: DataStore<Preferences>
        get() = app.applicationContext.dataStore

    private val appDirKey = stringPreferencesKey("appDir")
    private val startOnPolicy = stringPreferencesKey("startOnPolicy")
    private val favoriteMaps = stringSetPreferencesKey("favoriteMaps")
    private val rotationMode = stringPreferencesKey("rotationMode")
    private val speedVisibility = booleanPreferencesKey("speedVisibility")
    private val orientationVisibility = booleanPreferencesKey("orientationVisibility")
    private val gpsDataVisibility = booleanPreferencesKey("gpsDataVisibility")
    private val magnifyingFactor = intPreferencesKey("magnifyingFactor")
    private val maxScale = floatPreferencesKey("maxScale")
    private val unitForBeaconRadius = stringPreferencesKey("unitForBeaconRadius")
    private val lastMapId = stringPreferencesKey("lastMapId")
    private val defineScaleWhenCentered = booleanPreferencesKey("defineScaleWhenCentered")
    private val showScaleIndicator = booleanPreferencesKey("showScaleIndicator")
    private val showZoomIndicator = booleanPreferencesKey("showZoomIndicator")
    private val scaleRatioCentered = floatPreferencesKey("scaleRatioCentered")
    private val measurementSystem = stringPreferencesKey("measurementSystem")
    private val locationProducerInfo = stringPreferencesKey("locationProducerInfo")
    private val trackFollowThreshold = intPreferencesKey("trackFollowThreshold")
    private val recordingExportFormat = stringPreferencesKey("recordingExportFormat")
    private val advancedSettings = booleanPreferencesKey("advancedSettings")

    /**
     * Get the current application directory as [File].
     * This implementation tries to get it from the configuration file. Since this file might have
     * been modified by an human, a check is done to fallback to the default application directory
     * if the path isn't among the list of possible paths.
     * It's also a security in the case the application directories change across versions.
     */
    fun getAppDir(): Flow<File?> {
        return dataStore.safeData.map { pref ->
            pref[appDirKey]?.let { if (checkAppPath(it)) File(it) else null }
                ?: trekMeContext.defaultAppDir.firstOrNull()
        }
    }

    /**
     * Set the application directory - the folder in which new maps are downloaded and also the
     * folder walked on app startup to fetch the list of maps.
     */
    suspend fun setAppDir(file: File) {
        if (checkAppPath(file.absolutePath)) {
            dataStore.safeEdit { settings ->
                settings[appDirKey] = file.absolutePath
            }
        }
    }

    private fun checkAppPath(path: String): Boolean {
        return trekMeContext.rootDirListFlow.value.map {
            it.absolutePath
        }.contains(path)
    }

    /**
     * The [StartOnPolicy] defines whether TrekMe should boot on the map list or on the last map.
     */
    fun getStartOnPolicy(): Flow<StartOnPolicy> {
        return dataStore.safeData.map { pref ->
            pref[startOnPolicy]?.let { StartOnPolicy.valueOf(it) } ?: StartOnPolicy.MAP_LIST
        }
    }

    suspend fun setStartOnPolicy(policy: StartOnPolicy) {
        dataStore.safeEdit { settings ->
            settings[startOnPolicy] = policy.name
        }
    }

    fun getMagnifyingFactor(): Flow<Int> = dataStore.safeData.map { it[magnifyingFactor] ?: 0 }

    suspend fun setMagnifyingFactor(factor: Int) {
        dataStore.safeEdit {
            it[magnifyingFactor] = factor
        }
    }

    fun getMaxScale(): Flow<Float> = dataStore.safeData.map { it[maxScale] ?: 2f }

    suspend fun setMaxScale(scale: Float) {
        dataStore.safeEdit {
            it[maxScale] = scale
        }
    }

    fun getUnitForBeaconRadius(): Flow<DistanceUnit> {
        return dataStore.safeData.map { pref ->
            pref[unitForBeaconRadius]?.let { DistanceUnit.valueOf(it) } ?: DistanceUnit.Meters
        }
    }

    suspend fun setUnitForBeaconRadius(unit: DistanceUnit) {
        dataStore.safeEdit { settings ->
            settings[unitForBeaconRadius] = unit.name
        }
    }

    /**
     * Get the rotation behavior when viewing a map.
     */
    fun getRotationMode(): Flow<RotationMode> {
        return dataStore.safeData.map { pref ->
            pref[rotationMode]?.let { RotationMode.valueOf(it) } ?: RotationMode.NONE
        }
    }

    suspend fun setRotationMode(mode: RotationMode) {
        dataStore.safeEdit {
            it[rotationMode] = mode.name
            if (mode == RotationMode.FOLLOW_ORIENTATION) {
                it[orientationVisibility] = true
            }
        }
    }

    fun getSpeedVisibility(): Flow<Boolean> = dataStore.safeData.map { it[speedVisibility] ?: false }

    suspend fun setSpeedVisibility(v: Boolean) {
        dataStore.safeEdit {
            it[speedVisibility] = v
        }
    }

    suspend fun toggleSpeedVisibility() {
        dataStore.safeEdit {
            it[speedVisibility] = !(it[speedVisibility] ?: false)
        }
    }

    suspend fun toggleOrientationVisibility() {
        dataStore.safeEdit {
            it[orientationVisibility] = !(it[orientationVisibility] ?: false)
        }
    }

    suspend fun setOrientationVisibility(v: Boolean) {
        dataStore.safeEdit {
            it[orientationVisibility] = v
        }
    }

    fun getOrientationVisibility(): Flow<Boolean> = dataStore.safeData.map {
        it[orientationVisibility] ?: false
    }

    fun getGpsDataVisibility(): Flow<Boolean> = dataStore.safeData.map {
        it[gpsDataVisibility] ?: false
    }

    suspend fun setGpsDataVisibility(v: Boolean) {
        dataStore.safeEdit {
            it[gpsDataVisibility] = v
        }
    }

    suspend fun toggleGpsDataVisibility() {
        dataStore.safeEdit {
            it[gpsDataVisibility] = !(it[gpsDataVisibility] ?: false)
        }
    }

    /**
     * If `true`, [scaleRatioCentered] is accounted for. Otherwise, [scaleRatioCentered] is ignored.
     */
    fun getDefineScaleCentered(): Flow<Boolean> = dataStore.safeData.map {
        it[defineScaleWhenCentered] ?: true
    }

    suspend fun setDefineScaleCentered(defined: Boolean) {
        dataStore.safeEdit {
            it[defineScaleWhenCentered] = defined
        }
    }

    fun getShowScaleIndicator(): Flow<Boolean> = dataStore.safeData.map {
        it[showScaleIndicator] ?: true
    }

    suspend fun setShowScaleIndicator(show: Boolean) {
        dataStore.safeEdit {
            it[showScaleIndicator] = show
        }
    }

    fun getShowZoomIndicator(): Flow<Boolean> = dataStore.safeData.map {
        it[showZoomIndicator] ?: false
    }

    suspend fun setShowZoomIndicator(show: Boolean) {
        dataStore.safeEdit {
            it[showZoomIndicator] = show
        }
    }

    /**
     * The scale ratio in percent (between 0f and the max allowed scale) at which the MapView is set
     * when centering on the current position.
     * By default, the max scale is 2f and the scale ratio is 50f.
     */
    fun getScaleRatioCentered(): Flow<Float> {
        return dataStore.safeData.map {
            it[scaleRatioCentered] ?: 50f
        }
    }

    suspend fun setScaleRatioCentered(scale: Float) {
        dataStore.safeEdit {
            it[scaleRatioCentered] = scale
        }
    }

    fun getMeasurementSystem(): Flow<MeasurementSystem> {
        return dataStore.safeData.map { pref ->
            pref[measurementSystem]?.let {
                when (it) {
                    MeasurementSystem.METRIC.name -> MeasurementSystem.METRIC
                    MeasurementSystem.IMPERIAL.name -> MeasurementSystem.IMPERIAL
                    else -> MeasurementSystem.METRIC
                }
            } ?: MeasurementSystem.METRIC
        }
    }

    suspend fun setMeasurementSystem(system: MeasurementSystem) {
        dataStore.safeEdit {
            it[measurementSystem] = system.name
        }
    }

    /**
     * The ids of maps which are marked as favorites.
     */
    fun getFavoriteMapIds(): Flow<List<UUID>> {
        return dataStore.safeData.map { pref ->
            pref[favoriteMaps]?.let {
                it.mapNotNull { id ->
                    runCatching { UUID.fromString(id) }.getOrNull()
                }
            } ?: listOf()
        }
    }

    suspend fun setFavoriteMapIds(ids: List<UUID>) {
        dataStore.safeEdit { settings ->
            settings[favoriteMaps] = ids.map { id -> id.toString() }.toSet()
        }
    }

    /**
     * @return The last map id, or null if it's undefined.
     */
    fun getLastMapId(): Flow<UUID?> {
        return dataStore.safeData.map { pref ->
            runCatching {
                pref[lastMapId]?.let { id ->
                    if (id != "") UUID.fromString(id) else null
                }
            }.getOrNull()
        }
    }

    /**
     * Set and saves the last map id, for further use.
     */
    suspend fun setLastMapId(id: UUID) {
        dataStore.safeEdit {
            it[lastMapId] = id.toString()
        }
    }

    fun getLocationProducerInfo(): Flow<LocationProducerInfo> {
        return dataStore.safeData.map { pref ->
            pref[locationProducerInfo]?.let {
                runCatching {
                    Json.decodeFromString<LocationProducerInfo>(it)
                }.getOrNull()
            } ?: InternalGps
        }
    }

    /**
     * Set the active location producer.
     */
    suspend fun setLocationProducerInfo(info: LocationProducerInfo) {
        dataStore.safeEdit {
            it[locationProducerInfo] = Json.encodeToString(info)
        }
    }

    /**
     * Get the threshold, in meters.
     */
    fun getTrackFollowThreshold(): Flow<Int> {
        return dataStore.safeData.map { it[trackFollowThreshold] ?: 50 }
    }

    suspend fun setTrackFollowThreshold(valueInMeters: Int) {
        dataStore.safeEdit {
            it[trackFollowThreshold] = valueInMeters
        }
    }

    fun getAdvancedSettings(): Flow<Boolean> {
        return dataStore.safeData.map { pref ->
            pref[advancedSettings] ?: false
        }
    }

    suspend fun setAdvancedSettings(enabled: Boolean) {
        dataStore.safeEdit { settings ->
            settings[advancedSettings] = enabled
        }
    }

    /**
     * The [GeoRecordExportFormat] defines which foreign export format is used when exporting
     * a recording using the share button in the recordings screen.
     * When `null`, the native format is used (gpx).
     */
    fun getRecordingExportFormat(): Flow<GeoRecordExportFormat> {
        return dataStore.safeData.map { pref ->
            pref[recordingExportFormat]?.let { GeoRecordExportFormat.valueOf(it) } ?: GeoRecordExportFormat.Gpx
        }
    }

    suspend fun setRecordingExportFormat(format: GeoRecordExportFormat) {
        dataStore.safeEdit { settings ->
            settings[recordingExportFormat] = format.name
        }
    }
}

enum class StartOnPolicy {
    MAP_LIST, LAST_MAP
}

enum class RotationMode {
    NONE, FOLLOW_ORIENTATION, FREE
}

private const val SETTINGS = "settings"