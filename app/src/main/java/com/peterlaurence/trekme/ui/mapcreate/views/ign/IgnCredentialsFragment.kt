package com.peterlaurence.trekme.ui.mapcreate.views.ign

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AlertDialog
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import android.text.method.LinkMovementMethod
import android.widget.TextView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.IGNCredentials
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.core.providers.bitmap.checkIgnProvider
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSelectedEvent
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.CoroutineContext

/**
 * This fragment is for filling IGN France credentials:
 * * user
 * * password
 * * API key
 *
 * It warns the user if any error arises.
 *
 * @author peterLaurence on 14/04/18
 */
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

        val ignUserPreference = findPreference<EditTextPreference>(getString(R.string.ign_user))
        ignUserPreference?.text = ignUser
        ignUserPreference?.setOnPreferenceChangeListener { _, ignUser ->
            this.ignUser = ignUser as String
            saveCredentials()
            true
        }

        val ignPwdPreference = findPreference<EditTextPreference>(getString(R.string.ign_pwd))
        ignPwdPreference?.text = ignPwd
        ignPwdPreference?.setOnPreferenceChangeListener { _, ignPwd ->
            this.ignPwd = ignPwd as String
            saveCredentials()
            true
        }

        val ignApiKeyPreference = findPreference<EditTextPreference>(getString(R.string.ign_api_key))
        ignApiKeyPreference?.text = ignApiKey
        ignApiKeyPreference?.setOnPreferenceChangeListener { _, ignApiKey ->
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
                    afterIgnCredentialsSaved()
                } else {
                    /* Warn the user that we don't have storage rights */
                    showWarningDialog(getString(R.string.ign_warning_storage_rights))
                }
            }
        }
    }

    /**
     * We first try to download a single tile, using the credentials. This is done inside a coroutine
     * which is cancelled if this fragment is left before this check completes.
     */
    private fun CoroutineScope.afterIgnCredentialsSaved() = launch {
        /* Test IGN credentials */
        val isOk = async(Dispatchers.IO) {
            checkIgnProvider(ignApiKey, ignUser, ignPwd)
        }

        /* Then, either invite to proceed to map creation, or show a warning */
        if (isOk.await()) {
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