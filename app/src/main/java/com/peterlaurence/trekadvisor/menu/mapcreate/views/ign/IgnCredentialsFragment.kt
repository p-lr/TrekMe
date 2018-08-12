package com.peterlaurence.trekadvisor.menu.mapcreate.views.ign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.preference.EditTextPreference
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

        /* Init values from the credentials */
        val ignCredentials = MapSourceCredentials.getIGNCredentials()
        ignUser = ignCredentials?.user ?: ""
        ignPwd = ignCredentials?.pwd ?: ""
        ignApiKey = ignCredentials?.api ?: ""

        val ignUserPreference = findPreference(getString(R.string.ign_user)) as EditTextPreference
        ignUserPreference.text = ignUser
        ignUserPreference.setOnPreferenceChangeListener { _, ignUser ->
            this.ignUser = ignUser as String
            saveCredentials()
            true
        }

        val ignPwdPreference = findPreference(getString(R.string.ign_pwd)) as EditTextPreference
        ignPwdPreference.text = ignPwd
        ignPwdPreference.setOnPreferenceChangeListener { _, ignPwd ->
            this.ignPwd = ignPwd as String
            saveCredentials()
            true
        }

        val ignApiKeyPreference = findPreference(getString(R.string.ign_api_key)) as EditTextPreference
        ignApiKeyPreference.text = ignApiKey
        ignApiKeyPreference.setOnPreferenceChangeListener { _, ignApiKey ->
            this.ignApiKey = ignApiKey as String
            saveCredentials()
            true
        }
    }

    private fun saveCredentials() {
        if (this::ignUser.isInitialized && this::ignPwd.isInitialized && this::ignApiKey.isInitialized) {
            MapSourceCredentials.saveIGNCredentials(IGNCredentials(ignUser, ignPwd, ignApiKey))
        }
    }

    override fun onStart() {
        super.onStart()

        /* Display the link to the IGN API tutorial */
        val snackBar = Snackbar.make(view!!, R.string.ign_help_msg, Snackbar.LENGTH_INDEFINITE)
        snackBar.setAction(R.string.ign_help_action) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.ign_help_link)))
            startActivity(browserIntent)
        }
        snackBar.show()
    }
}