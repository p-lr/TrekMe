package com.peterlaurence.trekme.ui.settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.viewmodel.settings.SettingsViewModel


class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var viewModel: SettingsViewModel

    private val downloadPref: ListPreference?
        get() = preferenceManager.findPreference(
                getString(R.string.preference_download_location_key))

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

        viewModel = ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        initComponents()

        /* Observe the changes in the model */
        viewModel.getDownloadDirList().observe(this, Observer<List<String>> {
            it?.let { dirs ->
                updateDownloadDirList(dirs.toTypedArray())
            }
        })

        viewModel.getDownloadDir().observe(this, Observer<String> {
            it?.let { path ->
                updateDownloadSelection(path)
            }
        })


    }

    private fun updateDownloadDirList(dirs: Array<String>) {
        downloadPref?.entries = dirs
        downloadPref?.entryValues = dirs
    }

    private fun updateDownloadSelection(path: String) {
        downloadPref?.setSummaryAndValue(path)
    }

    private fun initComponents() {
        downloadPref?.setOnPreferenceChangeListener { _, newValue ->
            val newPath = newValue as String
            viewModel.setDownloadDirPath(newPath)
            updateDownloadSelection(newPath)
            true
        }
    }

    private fun ListPreference.setSummaryAndValue(txt: String) = apply {
        summary = txt
        value = txt
    }


    companion object {
        const val TAG = "settingsFragment"
    }
}