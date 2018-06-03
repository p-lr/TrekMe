package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceCredentials

class IgnCredentialsFragment : PreferenceFragmentCompat() {
    private lateinit var ignUser: String
    private lateinit var ignPwd: String
    private lateinit var ignApiKey: String


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.ign_credentials_settings)

        val ignUserPreference = findPreference(getString(R.string.ign_user))
        ignUserPreference.setOnPreferenceChangeListener { _, ignUser ->
            this.ignUser = ignUser as String
            saveCredentials()
            true
        }

        val ignPwdPreference = findPreference(getString(R.string.ign_pwd))
        ignPwdPreference.setOnPreferenceChangeListener { _, ignPwd ->
            this.ignPwd = ignPwd as String
            saveCredentials()
            true
        }

        val ignApiKeyPreference = findPreference(getString(R.string.ign_api_key))
        ignApiKeyPreference.setOnPreferenceChangeListener { _, ignApiKey ->
            this.ignApiKey = ignApiKey as String
            saveCredentials()
            true
        }
    }

    private fun saveCredentials() {
        if (this::ignUser.isInitialized && this::ignPwd.isInitialized && this::ignApiKey.isInitialized) {
            MapSourceCredentials.saveIGNCredentials(IGNCredentials(ignUser, ignPwd, ignApiKey))
            println("credentials saved")
        }
    }
}