package com.peterlaurence.trekadvisor.core.mapsource

import android.util.Log
import com.google.gson.GsonBuilder
import com.peterlaurence.trekadvisor.core.TrekAdvisorContext
import com.peterlaurence.trekadvisor.util.FileUtils
import java.io.File
import java.io.IOException
import java.io.PrintWriter

object MapSourceCredentials {
    val supportedMapSource = MapSource.values()
    val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private val TAG = javaClass.toString()
    private val configFile = File(TrekAdvisorContext.CREDENTIALS_DIR, "ign.json")

    lateinit var credentials: Credentials


    fun getIGNCredentials(): IGNCredentials? {
        val jsonString = try {
            if (configFile.exists()) {
                FileUtils.getStringFromFile(configFile)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
        credentials = gson.fromJson(jsonString, Credentials::class.java) ?: Credentials(null)
        return credentials.ignCredentials
    }

    fun saveIGNCredentials(ignCredentials: IGNCredentials): Boolean {
        credentials.ignCredentials = ignCredentials

        val jsonString = gson.toJson(credentials)

        return try {
            val writer = PrintWriter(configFile)
            writer.print(jsonString)
            writer.close()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error while saving the IGN credentials")
            Log.e(TAG, e.message, e)
            false
        }
    }
}

data class Credentials(var ignCredentials: IGNCredentials?)

data class IGNCredentials(val user: String?, val pwd: String?, val api: String?)

