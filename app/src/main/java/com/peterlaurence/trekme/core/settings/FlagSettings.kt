package com.peterlaurence.trekme.core.settings

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.peterlaurence.trekme.util.android.safeData
import com.peterlaurence.trekme.util.android.safeEdit
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This data store contains application flags, which are typically different from user settings.
 */
@Singleton
class FlagSettings @Inject constructor(
    private val app: Application
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = FLAG_SETTINGS)

    private val dataStore: DataStore<Preferences>
        get() = app.applicationContext.dataStore

    private val showTipForTrailSearch = booleanPreferencesKey("showTipForTrailSearch")

    suspend fun getShowTipForTrailSearch(): Boolean = dataStore.safeData.map {
        it[showTipForTrailSearch]
    }.catch {
        emit(false)
    }.firstOrNull() ?: true

    suspend fun setShowTipForTrailSearch(show: Boolean) {
        dataStore.safeEdit {
            it[showTipForTrailSearch] = show
        }
    }
}

private const val FLAG_SETTINGS = "flag_settings"