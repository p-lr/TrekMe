package com.peterlaurence.trekme.ui.settings

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.viewmodel.settings.SettingsViewModel


class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var viewModel: SettingsViewModel

    private val startOnPref: ListPreference?
        get() = preferenceManager.findPreference(getString(R.string.preference_starton_key))

    private val downloadPref: ListPreference?
        get() = preferenceManager.findPreference(getString(R.string.preference_download_location_key))

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

        viewModel = ViewModelProviders.of(activity!!).get(SettingsViewModel::class.java)
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

        viewModel.getStartOnPolicy().observe(this, Observer<StartOnPolicy> {
            it?.let { policy ->
                updateStartOnPolicy(policy)
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

    private fun updateStartOnPolicy(policy: StartOnPolicy) {
        val txt = when (policy) {
            StartOnPolicy.MAP_LIST -> getString(R.string.preference_starton_maplist)
            StartOnPolicy.LAST_MAP -> getString(R.string.preference_starton_lastmap)
        }

        startOnPref?.setSummaryAndValue(txt)
    }

    private fun initComponents() {
        downloadPref?.setOnPreferenceChangeListener { _, newValue ->
            val newPath = newValue as String
            viewModel.setDownloadDirPath(newPath)
            updateDownloadSelection(newPath)
            true
        }

        startOnPref?.setOnPreferenceChangeListener { _, newValue ->
            startOnPref?.setSummaryAndValue(newValue as String)
            val policy = when(newValue) {
                getString(R.string.preference_starton_maplist) -> StartOnPolicy.MAP_LIST
                getString(R.string.preference_starton_lastmap) -> StartOnPolicy.LAST_MAP
                else -> StartOnPolicy.MAP_LIST
            }
            viewModel.setStartOnPolicy(policy)
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