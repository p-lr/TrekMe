package com.peterlaurence.trekadvisor.core.mapsource

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.GsonBuilder

object MapSourceLoader {
    val supportedMapSource = MapSource.values()
    private val CREDENTIAL_KEY = "mapSourceCredentials"
    val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()

    fun getIGNCredentials(context: Context): IGNCredentials? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val jsonString = preferences.getString(CREDENTIAL_KEY, null)
        return gson.fromJson(jsonString, Credentials::class.java)?.ignCredentials
    }

    fun saveIGNCredentials(context: Context, ignCredentials: IGNCredentials?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context).edit()
        val jsonString = gson.toJson(ignCredentials)
        preferences.putString(CREDENTIAL_KEY, jsonString).apply()
    }
}

data class Credentials(val ignCredentials: IGNCredentials?)

data class IGNCredentials(val user: String?, val pwd: String?, val api: String?)

enum class MapSource {
    IGN, OPEN_STREET_MAP
}