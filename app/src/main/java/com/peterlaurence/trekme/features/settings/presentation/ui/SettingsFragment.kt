package com.peterlaurence.trekme.features.settings.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.settings.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global app settings user interface.
 *
 * @author P.Laurence on 05/05/19
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    private var startOnPref: ListPreference? = null
    private var measurementSystemPref: ListPreference? = null
    private var rootFolderPref: ListPreference? = null
    private var maxScalePref: ListPreference? = null
    private var magnifyingPref: ListPreference? = null
    private var rotationModePref: ListPreference? = null
    private var defineScaleCenteredPref: CheckBoxPreference? = null
    private var scaleCenteredPref: SeekBarPreference? = null
    private var showScaleIndicatorPref: CheckBoxPreference? = null
    private var trackFollowPref: ListPreference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.app_settings)

        initComponents()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.settings_frgmt_title)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.purchaseFlow.collect { purchased ->
                    trackFollowPref?.apply {
                        isVisible = purchased
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appDirListFlow.collect { dirs ->
                    updateDownloadDirList(dirs.toTypedArray())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.appDirFlow.collect { path ->
                    updateDownloadSelection(path)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startOnPolicyFlow.collect { policy ->
                    updateStartOnPolicy(policy)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.maxScaleFlow.collect { maxScale ->
                    updateMaxScale(maxScale)

                    mapFeatureEvents.mapScaleFlow.value?.let { scale ->
                        val scaleRatio = (scale * 100 / maxScale).toInt()
                        val str = getString(
                            R.string.preference_zoom_when_centered_compl,
                            scaleRatio.toString()
                        )
                        scaleCenteredPref?.title =
                            getString(R.string.preference_zoom_when_centered) + " " + str
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.magnifyingFactorFlow.collect {
                    updateMagnifyingFactor(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rotationModeFlow.collect {
                    updateRotationMode(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.defineScaleCenteredFlow.collect {
                    updateDefineScaleCentered(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scaleRatioCenteredFlow.collect {
                    updateScaleRatioCentered(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.measurementSystemFlow.collect {
                    updateMeasurementSystem(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showScaleIndicatorFlow.collect {
                    updateShowScaleIndicator(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trackFollowThreshold.collect {
                    updateTrackFollowThreshold(it)
                }
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
        /* Now that the measurement system changed, update the track-follow entries */
        setTrackFollowEntries()
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

    private fun updateShowScaleIndicator(show: Boolean) {
        showScaleIndicatorPref?.isChecked = show
    }

    private fun updateTrackFollowThreshold(valueInMeters: Int) {
        trackFollowPref?.apply {
            summary = UnitFormatter.formatDistance(valueInMeters.toDouble())
            value = valueInMeters.toString()
        }
    }

    private fun initComponents() {
        startOnPref = preferenceManager.findPreference(getString(R.string.preference_starton_key))
        measurementSystemPref =
            preferenceManager.findPreference(getString(R.string.preference_measurement_system))
        rootFolderPref =
            preferenceManager.findPreference(getString(R.string.preference_root_location_key))
        maxScalePref =
            preferenceManager.findPreference(getString(R.string.preference_max_scale_key))
        magnifyingPref =
            preferenceManager.findPreference(getString(R.string.preference_magnifying_key))
        rotationModePref =
            preferenceManager.findPreference(getString(R.string.preference_rotation_mode_key))
        defineScaleCenteredPref =
            preferenceManager.findPreference(getString(R.string.preference_change_scale_when_centering_key))
        scaleCenteredPref =
            preferenceManager.findPreference(getString(R.string.preference_zoom_when_centered_key))
        showScaleIndicatorPref =
            preferenceManager.findPreference(getString(R.string.preference_show_scale_indicator_key))
        trackFollowPref = preferenceManager.findPreference(getString(R.string.preference_track_follow_key))

        scaleCenteredPref?.title = getString(R.string.preference_zoom_when_centered)

        rootFolderPref?.setOnPreferenceChangeListener { _, newValue ->
            val newPath = newValue as String
            viewModel.setDownloadDirPath(newPath)
            updateDownloadSelection(newPath)
            true
        }

        startOnPref?.setOnPreferenceChangeListener { _, newValue ->
            val policy = when (newValue as String) {
                getString(R.string.preference_starton_maplist) -> StartOnPolicy.MAP_LIST
                getString(R.string.preference_starton_lastmap) -> StartOnPolicy.LAST_MAP
                else -> StartOnPolicy.MAP_LIST
            }
            viewModel.setStartOnPolicy(policy)
            true
        }

        measurementSystemPref?.setOnPreferenceChangeListener { _, newValue ->
            val system = when (newValue as String) {
                getString(R.string.metric_system) -> MeasurementSystem.METRIC
                getString(R.string.imperial_system) -> MeasurementSystem.IMPERIAL
                else -> MeasurementSystem.METRIC
            }
            viewModel.setMeasurementSystem(system)
            true
        }

        maxScalePref?.setOnPreferenceChangeListener { _, newValue ->
            val scale = (newValue as String).toFloat()
            viewModel.setMaxScale(scale)
            true
        }

        magnifyingPref?.setOnPreferenceChangeListener { _, newValue ->
            val factor = (newValue as String).toInt()
            viewModel.setMagnifyingFactor(factor)
            true
        }

        rotationModePref?.setOnPreferenceChangeListener { _, newValue ->
            val rotationMode = when (newValue as String) {
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

        showScaleIndicatorPref?.setOnPreferenceChangeListener { _, v ->
            val checked = v as Boolean
            viewModel.setShowScaleIndicator(checked)
            true
        }

        setTrackFollowEntries()
        trackFollowPref?.setOnPreferenceChangeListener { _, newValue ->
            val threshold= (newValue as String).toInt()
            viewModel.setTrackFollowThreshold(threshold)
            true
        }
    }

    private fun setTrackFollowEntries() {
        trackFollowPref?.entries = resources.getStringArray(R.array.track_follow_thresholds).map {
            UnitFormatter.formatDistance(it.toDouble())
        }.toTypedArray()
    }

    private fun ListPreference.setSummaryAndValue(txt: String) = apply {
        summary = txt
        value = txt
    }


    companion object {
        const val TAG = "settingsFragment"
    }
}