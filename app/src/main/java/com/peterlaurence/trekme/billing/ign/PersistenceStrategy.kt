package com.peterlaurence.trekme.billing.ign

import android.util.Log
import com.google.gson.Gson
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.encrypt.decrypt
import com.peterlaurence.trekme.util.encrypt.encrypt
import java.io.File
import javax.inject.Inject

/**
 * This persistence strategy takes a [LicenseInfo], converts it to json, encrypts it, and writes it
 * to a file.
 * The reverse operation reads the string from the file, decrypts it, then converts the json string
 * to a [LicenseInfo] instance.
 */
class PersistenceStrategy @Inject constructor(trekMeContext: TrekMeContext) {
    private val gson = Gson()
    private val keyStoreFile = File(trekMeContext.credentialsDir, "keystore")

    fun persist(licenseInfo: LicenseInfo) {
        try {
            val json = gson.toJson(licenseInfo).encrypt()
            FileUtils.writeToFile(json, keyStoreFile)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't persist licenseInfo to file")
            e.printStackTrace()
        }
    }

    /**
     * Attempt to retrieve the [LicenseInfo] from the file.
     * If anything is wrong, it returns null.
     */
    fun getLicenseInfo(): LicenseInfo? {
        return try {
            val st = FileUtils.getStringFromFile(keyStoreFile).decrypt()
            gson.fromJson(st, LicenseInfo::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't read licenseInfo to file")
            e.printStackTrace()
            null
        }
    }
}

/**
 * @param purchaseTimeMillis purchase time in milliseconds since the epoch (Jan 1, 1970)
 */
data class LicenseInfo(val purchaseTimeMillis: Long)

