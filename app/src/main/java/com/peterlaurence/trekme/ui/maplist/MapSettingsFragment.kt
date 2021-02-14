package com.peterlaurence.trekme.ui.maplist

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.maplist.events.MapImageImportResult
import com.peterlaurence.trekme.viewmodel.mapsettings.MapSettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

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
    private val viewModel: MapSettingsViewModel by activityViewModels()

    @Inject
    lateinit var mapLoader: MapLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            val mapId = args.getInt(ARG_MAP_ID)
            setMap(mapId)
        }

        lifecycleScope.launchWhenResumed {
            viewModel.mapImageImportEvents.collect {
                onMapImageImportResult(it)
            }
        }
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
     *
     * @param mapId the id of the [Map]
     */
    private fun setMap(mapId: Int) {
        val map = mapLoader.getMap(mapId)
        this.map = map
        if (map != null) {
            /* Choice is made to have the preference file name equal to the map name */
            preferenceManager.sharedPreferencesName = map.name
            initComponents()
        }
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
        val saveButton = preferenceManager.findPreference<Preference>(
                getString(R.string.preference_save_button_key))
        changeImageButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            /* Search for all documents available via installed storage providers */
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
            true
        }

        /* Set the summaries and the values of preferences according to the Map object */
        map?.also { map ->
            val projectionName = map.projectionName ?: "None"
            setListPreferenceSummaryAndValue(calibrationListPreference, projectionName)
            setListPreferenceSummaryAndValue(calibrationPointsNumberPreference, map.calibrationPointsNumber.toString())
            setEditTextPreferenceSummaryAndValue(mapNamePreference, map.name)
        }
        calibrationButton?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val direction = MapSettingsFragmentDirections.actionMapSettingsFragmentToMapCalibrationFragment()
            NavHostFragment.findNavController(this).navigate(direction)
            true
        }
        calibrationPointsNumberPreference?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, newValue: Any? ->
            map?.let { map ->
                when (newValue as String?) {
                    "2" -> map.calibrationMethod = MapLoader.CalibrationMethod.SIMPLE_2_POINTS
                    "3" -> map.calibrationMethod = MapLoader.CalibrationMethod.CALIBRATION_3_POINTS
                    "4" -> map.calibrationMethod = MapLoader.CalibrationMethod.CALIBRATION_4_POINTS
                    else -> map.calibrationMethod = MapLoader.CalibrationMethod.SIMPLE_2_POINTS
                }
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
                /* If the projection is set to none */
                if (getString(R.string.projection_none) == projectionName) {
                    map?.projection = null
                    return@OnPreferenceChangeListener true
                }
                val map = this.map
                val saveMsg: String = if (map != null && mapLoader.mutateMapProjection(map, (projectionName as String))) {
                    getString(R.string.calibration_projection_saved_ok)
                } else {
                    getString(R.string.calibration_projection_error)
                }
                showMessage(saveMsg)
                true
            } catch (e: Exception) {
                false
            }
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
                        startActivityForResult(intent, MAP_SAVE_CODE)
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
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* Check if the request code is the one we are interested in */
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                val map = this.map
                if (uri != null && map != null) {
                    viewModel.setMapImage(map, uri)
                }
            }
        }

        /* After the user selected a folder in which to archive a map, call the relevant view-model */
        if (requestCode == MAP_SAVE_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) return
            val uri = data.data ?: return
            map?.also {
                viewModel.archiveMap(it, uri)
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
            saveChanges()
        }
    }

    /**
     * Save the Map content
     */
    private fun saveChanges() {
        map?.also {
            viewModel.saveMap(it)
        }
    }

    companion object {
        private const val ARG_MAP_ID = "mapId"
        private const val IMAGE_REQUEST_CODE = 1338
        private const val MAP_SAVE_CODE = 3465

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