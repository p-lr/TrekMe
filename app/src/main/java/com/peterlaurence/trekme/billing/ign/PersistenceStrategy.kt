package com.peterlaurence.trekme.billing.ign

import android.app.Application
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.peterlaurence.trekme.util.stackTraceAsString
import javax.inject.Inject
import javax.inject.Singleton


/**
 * This persistence strategy wraps encrypted serialization and deserialization logic of [LicenseInfo].
 * Leverages Jetpack Security with [EncryptedSharedPreferences].
 *
 * @author P.Laurence on 11/08/2020
 */
@Singleton
class PersistenceStrategy @Inject constructor(private val application: Application) {
    private val encryptedSharedPrefsName = "encryptedPrefs"
    private val keyStore = "keystoreTrekMe"
    private val licenseInfoPurchaseTimeKey = "licenseInfo.purchaseTimeMillis"

    private var masterKey: MasterKey? = null
        get() {
            if (field != null) return field
            field = try {
                val spec = KeyGenParameterSpec.Builder(
                        keyStore,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()

                MasterKey.Builder(application.applicationContext, keyStore)
                        .setKeyGenParameterSpec(spec)
                        .build()
            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceAsString())
                null
            }
            return field
        }

    private var sharedPreferences: SharedPreferences? = null
        get() {
            if (field != null) return field
            return masterKey?.let { masterKey ->
                try {
                    EncryptedSharedPreferences.create(
                            application.applicationContext,
                            encryptedSharedPrefsName,
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e: Exception) {
                    /* If the encrypted shared preference file comes from a previous installation
                     * of the app, it can't be read back. So just remove it now.
                     * The next time we access this property (to persist something), the file will
                     * be successfully created. */
                    application.applicationContext.deleteSharedPreferences(encryptedSharedPrefsName)
                    Log.e(TAG, e.stackTraceAsString())
                    null
                }
            }
        }

    fun persist(licenseInfo: LicenseInfo) {
        try {
            sharedPreferences?.edit {
                putLong(licenseInfoPurchaseTimeKey, licenseInfo.purchaseTimeMillis)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't persist licenseInfo")
            e.printStackTrace()
        }
    }

    /**
     * Attempt to retrieve the [LicenseInfo].
     * If anything is wrong, it returns null.
     */
    fun getLicenseInfo(): LicenseInfo? {
        return try {
            sharedPreferences?.getLong(licenseInfoPurchaseTimeKey, -1)?.let { time ->
                if (time != -1L) LicenseInfo(time) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Couldn't read licenseInfo")
            e.printStackTrace()
            null
        }
    }
}

private const val TAG = "PersistenceStrategy"

/**
 * @param purchaseTimeMillis purchase time in milliseconds since the epoch (Jan 1, 1970)
 */
data class LicenseInfo(val purchaseTimeMillis: Long)

