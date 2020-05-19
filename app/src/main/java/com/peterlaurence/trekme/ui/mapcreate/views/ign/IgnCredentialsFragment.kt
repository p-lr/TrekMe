package com.peterlaurence.trekme.ui.mapcreate.views.ign

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.IGNCredentials
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.core.providers.bitmap.checkIgnProvider
import com.peterlaurence.trekme.core.providers.layers.IgnClassic
import com.peterlaurence.trekme.model.providers.stream.createTileStreamProvider
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSelectedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

/**
 * This fragment is for filling IGN France credentials:
 * * user
 * * password
 * * API key
 *
 * It warns the user in case of any error.
 *
 * @author peterLaurence on 14/04/18
 */
class IgnCredentialsFragment : PreferenceFragmentCompat() {
    private var ignUser: String = ""
    private var ignPwd: String = ""
    private var ignApiKey: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.ign_credentials_settings)

        /* Init values from the credentials */
        val ignCredentials = MapSourceCredentials.getIGNCredentials()
        ignUser = ignCredentials?.user ?: ""
        ignPwd = ignCredentials?.pwd ?: ""
        ignApiKey = ignCredentials?.api ?: ""

        val ignUserPreference = findPreference<EditTextPreference>(getString(R.string.ign_user))
        ignUserPreference?.text = ignUser
        ignUserPreference?.setOnPreferenceChangeListener { _, ignUser ->
            this.ignUser = ignUser as String
            saveAndTest()
            true
        }

        val ignPwdPreference = findPreference<EditTextPreference>(getString(R.string.ign_pwd))
        ignPwdPreference?.text = ignPwd
        ignPwdPreference?.setOnPreferenceChangeListener { _, ignPwd ->
            this.ignPwd = ignPwd as String
            saveAndTest()
            true
        }

        val ignApiKeyPreference = findPreference<EditTextPreference>(getString(R.string.ign_api_key))
        ignApiKeyPreference?.text = ignApiKey
        ignApiKeyPreference?.setOnPreferenceChangeListener { _, ignApiKey ->
            this.ignApiKey = ignApiKey as String
            saveAndTest()
            true
        }
    }

    private fun saveAndTest() = lifecycleScope.launch {
        saveCredentials()
        testIgnCredentials()
    }

    private suspend fun saveCredentials() {
        val ignCredentials = IGNCredentials(ignUser, ignPwd, ignApiKey)
        MapSourceCredentials.saveIGNCredentials(ignCredentials).let { success ->
            if (!success && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                /* Warn the user that we don't have storage rights */
                showWarningDialog(getString(R.string.ign_warning_storage_rights))
            }
        }
    }

    /**
     * The test consists in downloading a single tile, using the credentials.
     */
    private suspend fun testIgnCredentials() {
        /* Skip the test if one or several of the fields are empty */
        if (ignUser == "" || ignPwd == "" || ignApiKey == "") return

        /* Test IGN credentials */
        val isOk = withContext(Dispatchers.IO) {
            val tileStreamProvider = try {
                createTileStreamProvider(MapSource.IGN, IgnClassic.realName)
            } catch (e: Exception) {
                return@withContext false
            }
            checkIgnProvider(tileStreamProvider)
        }

        /* Then, either invite to proceed to map creation, or show a warning */
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (isOk) {
            view?.let {
                val snackBar = Snackbar.make(it, R.string.ign_snackbar_continue, Snackbar.LENGTH_LONG)
                snackBar.setAction(R.string.ok_dialog) {
                    EventBus.getDefault().post(MapSourceSelectedEvent(MapSource.IGN))
                }
                snackBar.show()
            }
        } else {
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
        builder?.let {
            val view = layoutInflater.inflate(R.layout.ign_warning, null)
            view.findViewById<TextView>(R.id.ign_warning_msg).text = message
            view.findViewById<TextView>(R.id.ign_warning_help_link).movementMethod = LinkMovementMethod.getInstance()
            it.setView(view)
        }

        val dialog: AlertDialog? = builder?.create()
        dialog?.show()
    }
}