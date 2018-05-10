package com.peterlaurence.trekadvisor.core.mapsource

import android.util.Log
import com.google.gson.GsonBuilder
import com.peterlaurence.trekadvisor.core.TrekAdvisorContext
import com.peterlaurence.trekadvisor.util.FileUtils
import java.io.File
import java.io.IOException
import java.io.PrintWriter

object MapSourceLoader {
    val supportedMapSource = MapSource.values()
    val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val TAG = javaClass.toString()
    private val configFile = File(TrekAdvisorContext.CREDENTIALS_DIR, "ign.json")

    lateinit var credentials: Credentials


    fun getIGNCredentials(): IGNCredentials? {
        val jsonString = FileUtils.getStringFromFile(configFile)
        credentials = gson.fromJson(jsonString, Credentials::class.java)
        return credentials.ignCredentials
    }

    fun saveIGNCredentials(ignCredentials: IGNCredentials) {
        credentials.ignCredentials = ignCredentials

        val jsonString = gson.toJson(credentials)

        try {
            val writer = PrintWriter(configFile)
            writer.print(jsonString)
            writer.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error while saving the IGN credentials")
            Log.e(TAG, e.message, e)
        }
    }
}

data class Credentials(var ignCredentials: IGNCredentials?)

data class IGNCredentials(val user: String?, val pwd: String?, val api: String?)

enum class MapSource {
    IGN, OPEN_STREET_MAP
}