package com.peterlaurence.trekme.util.android

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/**
 * When an [IOException] is encountered when reading data, this extension property returns an empty
 * [Preferences] instance.
 */
val <T> DataStore<T>.safeData: Flow<T>
    get() = data.catch {
        if (it is IOException) {
            emptyPreferences()
        } else throw it
    }

suspend fun DataStore<Preferences>.safeEdit(
    transform: suspend (MutablePreferences) -> Unit
): Preferences? {
    return runCatching {
        this@safeEdit.edit(transform)
    }.getOrNull()
}