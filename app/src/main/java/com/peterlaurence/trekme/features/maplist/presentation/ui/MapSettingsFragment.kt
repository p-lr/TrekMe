package com.peterlaurence.trekme.features.maplist.presentation.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navGraphViewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.maplist.presentation.events.MapImageImportResult
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import com.peterlaurence.trekme.util.isFrench
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Fragment that shows the settings for a given map. It provides the abilities to :
 *
 *  * Change map thumbnail image
 *  * Map calibration
 *
 *  * Choose the projection
 *  * Define the number of calibration point
 *  * Define the calibration points
 *
 *  * Change the map name
 *  * Save the map
 *
 * @author P.Laurence on 16/04/16 - Converted to Kotlin on 11/11/2020
 */
@AndroidEntryPoint
class MapSettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    private var map: Map? = null
    private val viewModel: MapSettingsViewModel by navGraphViewModels(R.id.map_settings_graph) {
        defaultViewModelProviderFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.map_settings_frgmt_title)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.mapFlow.filterNotNull().collect { map ->
                    setMap(map)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.mapImageImportEvents.collect {
                    onMapImageImportResult(it)
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        /* The Preferences layout */
        addPreferencesFromResource(R.xml.calibration_settings)
    }

    /**
     * Set the [Map] for this `MapSettingsFragment`.
     *
     * This calls `initComponents` method, so it must be called after the Preferences
     * layout has been set with e.g `addPreferencesFromResource`.
     */
    private fun setMap(map: Map) {
        this.map = map
        /* Choice is made to have the preference file name equal to the map name, from which
         * we strip reserved characters */
        preferenceManager.sharedPreferencesName = map.name.replace("[\\\\/:*?\"<>|]".toRegex(), "")
        initComponents()
    }

    private fun initComponents() {
        val changeImageButton = preferenceManager.findPreference<Preference>(
                getString(R.string.preference_change_image_key))
        val calibrationListPreference = preferenceManager.findPreference<ListPreference>(
                getString(R.string.preference_projection_key))
        val calibrationPointsNumberPreference = preferenceManager.findPreference<ListPreference>(
                getString(R.string.preference_numpoints_key))
        val mapNamePreference = preferenceManager.findPreference<EditTextPreference>(
                getString(R.string.preference_map_title_key))
        val calibrationButton = preferenceManager.findPreference<Preference>(
                getString(R.string.preference_calibration_button_key))
        val sizePreference = preferenceManager.findPreference<Preference>(
                getString(R.string.preference_size_button_key)
        )
        val saveButton = preferenceManager.findPreference<Preference>(
                getString(R.string.preference_save_button_key))
        changeImageButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            /* Search for all documents available via installed storage providers */
            intent.type = "image/*"
            imageChangeResultLauncher.launch(intent)
            true
        }

        /* Set the summaries and the values of preferences according to the Map object */
        map?.also { map ->
            val projectionName = map.projectionName ?: "None"
            setListPreferenceSummaryAndValue(calibrationListPreference, projectionName)
            setListPreferenceSummaryAndValue(calibrationPointsNumberPreference, map.calibrationPointsNumber.toString())
            setEditTextPreferenceSummaryAndValue(mapNamePreference, map.name)

            val mapSize = map.sizeInBytes.value
            if (mapSize != null) {
                sizePreference?.title = getString(R.string.map_size_string)
                sizePreference?.summary = mapSize.formatSize()
            } else {
                sizePreference?.title = getString(R.string.map_size_compute_string)
            }
        }
        calibrationButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val direction =
                MapSettingsFragmentDirections.actionMapSettingsFragmentToCalibrationFragment()
            NavHostFragment.findNavController(this).navigate(direction)
            true
        }
        calibrationPointsNumberPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
            map?.let { map ->
                viewModel.setCalibrationPointsNumber(map, newValue as String?)
                true
            } ?: false
        }
        mapNamePreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue: Any? ->
            try {
                val newName = newValue as? String
                map?.also {
                    if (newName != null) viewModel.renameMap(it, newName)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
        calibrationListPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, projectionName: Any ->
            try {
                val map = this.map ?: return@OnPreferenceChangeListener false
                /* If the projection is set to none */
                if (getString(R.string.projection_none) == projectionName) {
                    viewModel.setProjection(map, null)
                    return@OnPreferenceChangeListener true
                }
                viewModel.setProjection(map, projectionName as String)
                showMessage(getString(R.string.calibration_projection_saved_ok))
                true
            } catch (e: Exception) {
                false
            }
        }
        sizePreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener { pref ->
            val map = this.map ?: return@OnPreferenceClickListener false
            if (map.sizeInBytes.value != null) return@OnPreferenceClickListener false
            pref.title = getString(R.string.map_size_computing)
            lifecycleScope.launchWhenStarted {
                viewModel.computeMapSize(map)?.also { sizeInBytes ->
                    pref.summary = sizeInBytes.formatSize()
                    pref.title = getString(R.string.map_size_string)
                }
            }
            true
        }
        saveButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val context = context ?: return@OnPreferenceClickListener true
            val builder = AlertDialog.Builder(requireActivity())
            val title = context.getString(R.string.archive_dialog_title)
            builder.setTitle(title)
                    .setMessage(R.string.archive_dialog_description)
                    .setPositiveButton(R.string.ok_dialog
                    ) { _, _ ->
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        mapSaveResultLauncher.launch(intent)
                    }
                    .setNegativeButton(R.string.cancel_dialog_string
                    ) { dialog: DialogInterface, _ -> dialog.dismiss() }
            val saveMapDialog = builder.create()
            saveMapDialog.show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
                ?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                ?.unregisterOnSharedPreferenceChangeListener(this)
    }

    /**
     * Converts a quantity expected to be in bytes into a [String]. For example:
     * 12_500 -> "12.5 Ko" (in French), or "12.5 KB" (in English)
     */
    private fun Long.formatSize(): String {
        val ctx = this@MapSettingsFragment.context ?: return this.toString()
        val byteStr = if (isFrench(ctx)) {
            "o"
        } else {
            "B"
        }
        var divider = 1.0
        val prefix = when {
            this < 1000 -> {
                ""
            }
            this < 500_000 -> {
                divider = 1000.0
                "K"
            }
            this < 500_000_000 -> {
                divider = 1000_000.0
                "M"
            }
            else -> {
                divider = 1_000_000_000.0
                "G"
            }
        }
        val number = BigDecimal(this / divider).setScale(2, RoundingMode.HALF_EVEN)
        return "$number $prefix$byteStr"
    }

    private val mapSaveResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /* After the user selected a folder in which to archive a map, call the relevant view-model */
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data == null) return@registerForActivityResult
            val uri = result.data?.data
            val map = this.map
            if (uri != null && map != null) {
                viewModel.archiveMap(map, uri)
            }
        }
    }

    private val imageChangeResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        /* Check if the request code is the one we are interested in */
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data == null) return@registerForActivityResult
            val uri = result.data?.data
            val map = this.map
            if (uri != null && map != null) {
                viewModel.setMapImage(map.id, uri)
            }
        }
    }

    private fun onMapImageImportResult(result: MapImageImportResult) {
        val msgId: Int = if (result.success) {
            R.string.map_image_import_ok
        } else {
            R.string.map_image_import_error
        }
        showMessage(getString(msgId))
    }

    private fun showMessage(msg: String) {
        val v = view
        if (v != null) {
            val snackbar = Snackbar.make(v, msg, Snackbar.LENGTH_SHORT)
            snackbar.show()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val pref = findPreference<Preference>(key)
        if (pref != null) {
            pref.summary = sharedPreferences.getString(key, "default")
        }
    }

    companion object {

        /* Convenience method */
        private fun setListPreferenceSummaryAndValue(preference: ListPreference?, value: String) {
            preference?.summary = value
            preference?.value = value
        }

        /* Convenience method */
        private fun setEditTextPreferenceSummaryAndValue(preference: EditTextPreference?, value: String) {
            preference?.summary = value
            preference?.text = value
        }
    }
}