package com.peterlaurence.trekadvisor.menu.mapcreate.providers.ign

import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.View
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceLoader

class IgnCredentialsFragment : PreferenceFragment() {
    private lateinit var ignUser: String
    private lateinit var ignPwd: String
    private lateinit var ignApiKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.ign_credentials_settings)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            MapSourceLoader.saveIGNCredentials(IGNCredentials(ignUser, ignPwd, ignApiKey))
            println("credentials saved")
        }
    }
}