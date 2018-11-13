package com.peterlaurence.trekadvisor.menu.mapcreate.views.ign

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.preference.EditTextPreference
import android.support.v7.preference.PreferenceFragmentCompat
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekadvisor.core.providers.generic.GenericBitmapProviderAuth
import com.peterlaurence.trekadvisor.core.providers.layers.IgnLayers
import com.peterlaurence.trekadvisor.core.providers.urltilebuilder.UrlTileBuilderIgn
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class IgnCredentialsFragment : PreferenceFragmentCompat(), CoroutineScope {
    private lateinit var ignUser: String
    private lateinit var ignPwd: String
    private lateinit var ignApiKey: String

    private val job: Job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onStop() {
        job.cancel()
        super.onStop()
    }

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
            val ignCredentials = IGNCredentials(ignUser, ignPwd, ignApiKey)
            MapSourceCredentials.saveIGNCredentials(ignCredentials).let { success ->
                if (success) {
                    testIgnCredentials()
                } else {
                    /* Warn the user that we don't have storage rights */
                    showWarningDialog(getString(R.string.ign_warning_storage_rights))
                }
            }
        }
    }

    private fun CoroutineScope.testIgnCredentials() = launch {
        val isOk = async(Dispatchers.IO) {
            val urlTileBuilder = UrlTileBuilderIgn(ignApiKey, IgnLayers.ScanExpressStandard.realName)
            val genericProvider = GenericBitmapProviderAuth(urlTileBuilder, ignUser, ignPwd)
            genericProvider.getBitmap(1, 1, 1) != null
        }

        if (!isOk.await()) {
            showWarningDialog(getString(R.string.ign_warning_credentials))
        }
    }

    /**
     * Warn the user: display a message.
     */
    private fun showWarningDialog(message: String) {
        val builder: AlertDialog.Builder? = activity?.let {
            AlertDialog.Builder(it)
        }

        builder?.setMessage(message)

        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }
}