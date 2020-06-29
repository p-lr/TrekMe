package com.peterlaurence.trekme.ui.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.viewmodel.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Global app settings are managed here.
 *
 * @author peterLaurence on 05/05/19
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private var startOnPref: ListPreference? = null
    private var rootFolderPref: ListPreference? = null
    private var magnifyingPref: ListPreference? = null
    private var rotationModePref: ListPreference? = null
    private var scaleCenteredPref: SeekBarPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

        initComponents()

        /* Observe the changes in the model */
        viewModel.appDirListLiveData.observe(this, Observer {
            it?.let { dirs ->
                updateDownloadDirList(dirs.toTypedArray())
            }
        })

        viewModel.appDirLiveData.observe(this, Observer {
            it?.let { path ->
                updateDownloadSelection(path)
            }
        })

        viewModel.startOnPolicyLiveData.observe(this, Observer {
            it?.let { policy ->
                updateStartOnPolicy(policy)
            }
        })

        viewModel.magnifyingFactorLiveData.observe(this, Observer {
            it?.let {
                updateMagnifyingFactor(it)
            }
        })

        viewModel.rotationModeLiveData.observe(this, Observer {
            it?.let {
                updateRotationMode(it)
            }
        })

        viewModel.scaleCentered.observe(this, Observer {
            it?.let {
                updateScaleCentered(it)
            }
        })
    }

    private fun updateDownloadDirList(dirs: Array<String>) {
        rootFolderPref?.entries = dirs
        rootFolderPref?.entryValues = dirs
    }

    private fun updateDownloadSelection(path: String) {
        rootFolderPref?.setSummaryAndValue(path)
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

    private fun updateRotationMode(mode: RotationMode) {
        val txt = when (mode) {
            RotationMode.NONE -> getString(R.string.preference_rotate_none)
            RotationMode.FREE -> getString(R.string.preference_rotate_free)
            RotationMode.FOLLOW_ORIENTATION -> getString(R.string.preference_rotate_with_orientation)
        }

        rotationModePref?.setSummaryAndValue(txt)
    }

    private fun updateScaleCentered(scaleCentered: Float) {
        scaleCenteredPref?.value = (scaleCentered * 50).toInt()
    }

    private fun initComponents() {
        startOnPref = preferenceManager.findPreference(getString(R.string.preference_starton_key))
        rootFolderPref = preferenceManager.findPreference(getString(R.string.preference_root_location_key))
        magnifyingPref = preferenceManager.findPreference(getString(R.string.preference_magnifying_key))
        rotationModePref = preferenceManager.findPreference(getString(R.string.preference_rotation_mode_key))
        scaleCenteredPref = preferenceManager.findPreference(getString(R.string.preference_scale_at_center_key))

        rootFolderPref?.setOnPreferenceChangeListener { _, newValue ->
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

        rotationModePref?.setOnPreferenceChangeListener { _, newValue ->
            rotationModePref?.setSummaryAndValue(newValue as String)
            val rotationMode = when (newValue) {
                getString(R.string.preference_rotate_with_orientation) -> RotationMode.FOLLOW_ORIENTATION
                getString(R.string.preference_rotate_none) -> RotationMode.NONE
                getString(R.string.preference_rotate_free) -> RotationMode.FREE
                else -> RotationMode.NONE
            }
            viewModel.setRotationMode(rotationMode)
            true
        }

        scaleCenteredPref?.min = 0
        scaleCenteredPref?.max = 100
        scaleCenteredPref?.showSeekBarValue = true
        scaleCenteredPref?.setOnPreferenceChangeListener { _, v ->
            /* Remarkable values:
             * SeekBar value -> Scale
             *           100 -> 2f
             *            50 -> 1f
             *             0 -> 0f
             */
            val value = v as Int
            val scaleFactor = value.toFloat() / 50f
            if (scaleFactor != viewModel.scaleCentered.value) {
                viewModel.setScaleCentered(scaleFactor)
            }
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