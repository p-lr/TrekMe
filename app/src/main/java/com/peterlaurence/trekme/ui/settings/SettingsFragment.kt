package com.peterlaurence.trekme.ui.settings

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.viewmodel.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Global app settings are managed here.
 *
 * @author P.Laurence on 05/05/19
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    private var startOnPref: ListPreference? = null
    private var measurementSystemPref: ListPreference? = null
    private var rootFolderPref: ListPreference? = null
    private var maxScalePref: ListPreference? = null
    private var magnifyingPref: ListPreference? = null
    private var rotationModePref: ListPreference? = null
    private var defineScaleCenteredPref: CheckBoxPreference? = null
    private var scaleCenteredPref: SeekBarPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

        initComponents()

        /* Observe the changes in the model */
        viewModel.appDirListLiveData.observe(this) {
            it?.let { dirs ->
                updateDownloadDirList(dirs.toTypedArray())
            }
        }

        viewModel.appDirLiveData.observe(this) {
            it?.let { path ->
                updateDownloadSelection(path)
            }
        }

        viewModel.startOnPolicyLiveData.observe(this) {
            it?.let { policy ->
                updateStartOnPolicy(policy)
            }
        }

        viewModel.measurementSystemLiveData.observe(this) {
            it?.let { updateMeasurementSystem(it) }
        }

        viewModel.maxScaleLiveData.observe(this) {
            it?.let {
                updateMaxScale(it)
            }
        }

        viewModel.magnifyingFactorLiveData.observe(this) {
            it?.let {
                updateMagnifyingFactor(it)
            }
        }

        viewModel.rotationModeLiveData.observe(this) {
            it?.let {
                updateRotationMode(it)
            }
        }

        viewModel.defineScaleCentered.observe(this) {
            it?.let {
                updateDefineScaleCentered(it)
            }
        }

        viewModel.scaleCentered.observe(this) {
            it?.let {
                updateScaleRatioCentered(it)
            }
        }
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

    private fun updateMeasurementSystem(system: MeasurementSystem) {
        val txt = when (system) {
            MeasurementSystem.METRIC -> getString(R.string.metric_system)
            MeasurementSystem.IMPERIAL -> getString(R.string.imperial_system)
        }

        measurementSystemPref?.setSummaryAndValue(txt)
    }

    private fun updateMaxScale(scale: Float) {
        maxScalePref?.setSummaryAndValue(scale.toInt().toString())
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

    private fun updateDefineScaleCentered(defined: Boolean) {
        defineScaleCenteredPref?.isChecked = defined
        scaleCenteredPref?.isVisible = defined
    }

    private fun updateScaleRatioCentered(scaleCentered: Float) {
        scaleCenteredPref?.value = scaleCentered.toInt()
    }

    private fun initComponents() {
        startOnPref = preferenceManager.findPreference(getString(R.string.preference_starton_key))
        measurementSystemPref = preferenceManager.findPreference(getString(R.string.preference_measurement_system))
        rootFolderPref = preferenceManager.findPreference(getString(R.string.preference_root_location_key))
        maxScalePref = preferenceManager.findPreference(getString(R.string.preference_max_scale_key))
        magnifyingPref = preferenceManager.findPreference(getString(R.string.preference_magnifying_key))
        rotationModePref = preferenceManager.findPreference(getString(R.string.preference_rotation_mode_key))
        defineScaleCenteredPref = preferenceManager.findPreference(getString(R.string.preference_change_scale_when_centering_key))
        scaleCenteredPref = preferenceManager.findPreference(getString(R.string.preference_zoom_when_centered_key))

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

        measurementSystemPref?.setOnPreferenceChangeListener { _, newValue ->
            measurementSystemPref?.setSummaryAndValue(newValue as String)
            val system = when (newValue) {
                getString(R.string.metric_system) -> MeasurementSystem.METRIC
                getString(R.string.imperial_system) -> MeasurementSystem.IMPERIAL
                else -> MeasurementSystem.METRIC
            }
            viewModel.setMeasurementSystem(system)
            true
        }

        maxScalePref?.setOnPreferenceChangeListener { _, newValue ->
            val strValue = newValue as String
            maxScalePref?.setSummaryAndValue(strValue)
            val scale = strValue.toFloat()
            viewModel.setMaxScale(scale)
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

        defineScaleCenteredPref?.isChecked = true
        defineScaleCenteredPref?.setOnPreferenceChangeListener { _, v ->
            val checked = v as Boolean
            viewModel.setDefineScaleCentered(checked)
            scaleCenteredPref?.isVisible = checked
            true
        }

        scaleCenteredPref?.min = 0
        scaleCenteredPref?.max = 100
        scaleCenteredPref?.showSeekBarValue = true
        scaleCenteredPref?.setOnPreferenceChangeListener { _, v ->
            val percent = (v as Int).toFloat()
            viewModel.setScaleRatioCentered(percent)
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