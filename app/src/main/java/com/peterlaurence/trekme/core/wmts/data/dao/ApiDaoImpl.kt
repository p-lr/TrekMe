package com.peterlaurence.trekme.core.wmts.data.dao

import android.annotation.SuppressLint
import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.dao.ApiDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiDaoImpl (private val appContext: Context): ApiDao {
    @Volatile
    private var keys: Keys? = null

    override suspend fun getIgnApi(): String? {
        return getKey { it?.ignApi }
    }

    override suspend fun getOrdnanceSurveyApi(): String? {
        return getKey { it?.ordnanceSurveyApi }
    }

    private suspend fun getKey(selector: (Keys?) -> String?): String? {
        val k = keys
        return if (k != null) {
            selector(k)
        } else {
            readKeys()
            selector(keys)
        }
    }

    @SuppressLint("ResourceType")
    private suspend fun readKeys() = withContext(Dispatchers.IO) {
        appContext.resources.openRawResource(R.drawable.takamaka).use {
            val bytes = it.readBytes()
            val keysStr = bytes.takeLast(65).toByteArray().toString(Charsets.UTF_8).split(';')
            if (keysStr.size == 2) {
                keys = Keys(keysStr[0], keysStr[1])
            }
        }
    }

    private data class Keys(val ignApi: String, val ordnanceSurveyApi: String)
}