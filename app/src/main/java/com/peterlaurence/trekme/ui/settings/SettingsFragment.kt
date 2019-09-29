package com.peterlaurence.trekme.ui.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.viewmodel.settings.SettingsViewModel

/**
 * Global app settings are managed here.
 *
 * @author peterLaurence on 05/05/19
 */
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private var startOnPref: ListPreference? = null
    private var downloadPref: ListPreference? = null
    private var magnifyingPref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

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

        viewModel.getMagnifyingFactorLiveData().observe(this, Observer<Int> {
            it?.let {
                updateMagnifyingFactor(it)
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

    private fun updateMagnifyingFactor(factor: Int) {
        magnifyingPref?.setSummaryAndValue(factor.toString())
    }

    private fun initComponents() {
        startOnPref = preferenceManager.findPreference(getString(R.string.preference_starton_key))
        downloadPref = preferenceManager.findPreference(getString(R.string.preference_download_location_key))
        magnifyingPref = preferenceManager.findPreference(getString(R.string.preference_magnifying_key))

        downloadPref?.setOnPreferenceChangeListener { _, newValue ->
            val newPath = newValue as String
            viewModel.setDownloadDirPath(newPath)
            updateDownloadSelection(newPath)
            true
        }

        startOnPref?.setOnPreferenceChangeListener { _, newValue ->
            startOnPref?.setSummaryAndValue(newValue as String)
            val policy = when (newValue) {
                getString(R.string.preference_starton_maplist) -> StartOnPolicy.MAP_LIST
                getString(R.string.preference_starton_lastmap) -> StartOnPolicy.LAST_MAP
                else -> StartOnPolicy.MAP_LIST
            }
            viewModel.setStartOnPolicy(policy)
            true
        }

        magnifyingPref?.setOnPreferenceChangeListener { _, newValue ->
            val strValue = newValue as String
            magnifyingPref?.setSummaryAndValue(strValue)
            val factor = strValue.toInt()
            viewModel.setMagnifyingFactor(factor)
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