package com.peterlaurence.trekme.core.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerInfo
import com.peterlaurence.trekme.core.units.DistanceUnit
import com.peterlaurence.trekme.core.units.MeasurementSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
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
    private val sharedPref: SharedPreferences =
        app.applicationContext.getSharedPreferences(oldSettingsFile, Context.MODE_PRIVATE)

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = settings,
        produceMigrations = { listOf(SharedPreferencesMigration({ sharedPref })) }
    )

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
    private val scaleRatioCentered = floatPreferencesKey("scaleRatioCentered")
    private val measurementSystem = stringPreferencesKey("measurementSystem")
    private val locationRationale = booleanPreferencesKey("locationDisclaimer")
    private val locationProducerInfo = stringPreferencesKey("locationProducerInfo")

    /**
     * Get the current application directory as [File].
     * This implementation tries to get it from the configuration file. Since this file might have
     * been modified by an human, a check is done to fallback to the default application directory
     * if the path isn't among the list of possible paths.
     * It's also a security in the case the application directories change across versions.
     */
    fun getAppDir(): Flow<File?> {
        return dataStore.data.map { pref ->
            pref[appDirKey]?.let { if (checkAppPath(it)) File(it) else null }
                ?: trekMeContext.defaultAppDir
        }
    }

    /**
     * Set the application directory - the folder in which new maps are downloaded and also the
     * folder walked on app startup to fetch the list of maps.
     */
    suspend fun setAppDir(file: File) {
        if (checkAppPath(file.absolutePath)) {
            dataStore.edit { settings ->
                settings[appDirKey] = file.absolutePath
            }
        }
    }

    private fun checkAppPath(path: String): Boolean {
        return trekMeContext.rootDirList?.map {
            it.absolutePath
        }?.contains(path) ?: false
    }

    /**
     * The [StartOnPolicy] defines whether TrekMe should boot on the map list or on the last map.
     */
    fun getStartOnPolicy(): Flow<StartOnPolicy> {
        return dataStore.data.map { pref ->
            pref[startOnPolicy]?.let { StartOnPolicy.valueOf(it) } ?: StartOnPolicy.MAP_LIST
        }
    }

    suspend fun setStartOnPolicy(policy: StartOnPolicy) {
        dataStore.edit { settings ->
            settings[startOnPolicy] = policy.name
        }
    }

    fun getMagnifyingFactor(): Flow<Int> = dataStore.data.map { it[magnifyingFactor] ?: 0 }

    suspend fun setMagnifyingFactor(factor: Int) {
        dataStore.edit {
            it[magnifyingFactor] = factor
        }
    }

    fun getMaxScale(): Flow<Float> = dataStore.data.map { it[maxScale] ?: 2f }

    suspend fun setMaxScale(scale: Float) {
        dataStore.edit {
            it[maxScale] = scale
        }
    }

    fun getUnitForBeaconRadius(): Flow<DistanceUnit> {
        return dataStore.data.map { pref ->
            pref[unitForBeaconRadius]?.let { DistanceUnit.valueOf(it) } ?: DistanceUnit.Meters
        }
    }

    suspend fun setUnitForBeaconRadius(unit: DistanceUnit) {
        dataStore.edit { settings ->
            settings[unitForBeaconRadius] = unit.name
        }
    }

    /**
     * Get the rotation behavior when viewing a map.
     */
    fun getRotationMode(): Flow<RotationMode> {
        return dataStore.data.map { pref ->
            pref[rotationMode]?.let { RotationMode.valueOf(it) } ?: RotationMode.NONE
        }
    }

    suspend fun setRotationMode(mode: RotationMode) {
        dataStore.edit {
            it[rotationMode] = mode.name
            if (mode == RotationMode.FOLLOW_ORIENTATION) {
                it[orientationVisibility] = true
            }
        }
    }

    fun getSpeedVisibility(): Flow<Boolean> = dataStore.data.map { it[speedVisibility] ?: false }

    suspend fun setSpeedVisibility(v: Boolean) {
        dataStore.edit {
            it[speedVisibility] = v
        }
    }

    suspend fun toggleSpeedVisibility() {
        dataStore.edit {
            it[speedVisibility] = !(it[speedVisibility] ?: false)
        }
    }

    suspend fun toggleOrientationVisibility() {
        dataStore.edit {
            it[orientationVisibility] = !(it[orientationVisibility] ?: false)
        }
    }

    suspend fun setOrientationVisibility(v: Boolean) {
        dataStore.edit {
            it[orientationVisibility] = v
        }
    }

    fun getOrientationVisibility(): Flow<Boolean> = dataStore.data.map {
        it[orientationVisibility] ?: false
    }

    fun getGpsDataVisibility(): Flow<Boolean> = dataStore.data.map {
        it[gpsDataVisibility] ?: false
    }

    suspend fun setGpsDataVisibility(v: Boolean) {
        dataStore.edit {
            it[gpsDataVisibility] = v
        }
    }

    suspend fun toggleGpsDataVisibility() {
        dataStore.edit {
            it[gpsDataVisibility] = !(it[gpsDataVisibility] ?: false)
        }
    }

    /**
     * If `true`, [scaleCentered] is accounted for. Otherwise, [scaleCentered] is ignored.
     */
    fun getDefineScaleCentered(): Flow<Boolean> = dataStore.data.map {
        it[defineScaleWhenCentered] ?: true
    }

    suspend fun setDefineScaleCentered(defined: Boolean) {
        dataStore.edit {
            it[defineScaleWhenCentered] = defined
        }
    }

    fun getShowScaleIndicator(): Flow<Boolean> = dataStore.data.map {
        it[showScaleIndicator] ?: true
    }

    suspend fun setShowScaleIndicator(show: Boolean) {
        dataStore.edit {
            it[showScaleIndicator] = show
        }
    }

    /**
     * The scale ratio in percent (between 0f and the max allowed scale) at which the MapView is set
     * when centering on the current position.
     * By default, the max scale is 2f and the scale ratio is 50f.
     */
    fun getScaleRatioCentered(): Flow<Float> {
        return dataStore.data.map {
            it[scaleRatioCentered] ?: 50f
        }
    }

    suspend fun setScaleRatioCentered(scale: Float) {
        dataStore.edit {
            it[scaleRatioCentered] = scale
        }
    }

    fun getMeasurementSystem(): Flow<MeasurementSystem> {
        return dataStore.data.map { pref ->
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
        dataStore.edit {
            it[measurementSystem] = system.name
        }
    }

    /**
     * The ids of maps which are marked as favorites.
     */
    fun getFavoriteMapIds(): Flow<List<UUID>> {
        return dataStore.data.map { pref ->
            pref[favoriteMaps]?.let {
                it.mapNotNull { id ->
                    runCatching { UUID.fromString(id) }.getOrNull()
                }
            } ?: listOf()
        }
    }

    suspend fun setFavoriteMapIds(ids: List<UUID>) {
        dataStore.edit { settings ->
            settings[favoriteMaps] = ids.map { id -> id.toString() }.toSet()
        }
    }

    /**
     * @return The last map id, or null if it's undefined.
     */
    fun getLastMapId(): Flow<UUID?> {
        return dataStore.data.map { pref ->
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
        dataStore.edit {
            it[lastMapId] = id.toString()
        }
    }

    fun isShowingLocationRationale(): Flow<Boolean> {
        return dataStore.data.map { it[locationRationale] ?: true }
    }

    suspend fun discardLocationDisclaimer() {
        dataStore.edit {
            it[locationRationale] = false
        }
    }

    fun getLocationProducerInfo(): Flow<LocationProducerInfo> {
        return dataStore.data.map { pref ->
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
        dataStore.edit {
            it[locationProducerInfo] = Json.encodeToString(info)
        }
    }
}

enum class StartOnPolicy {
    MAP_LIST, LAST_MAP
}

enum class RotationMode {
    NONE, FOLLOW_ORIENTATION, FREE
}

private const val oldSettingsFile = "trekmeSettings"
private const val settings = "settings"