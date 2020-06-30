package com.peterlaurence.trekme.ui.maplist;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

/**
 * Fragment that shows the settings for a given map. It provides the abilities to :
 * <ul>
 * <li>Calibrate the map</li>
 * <ul>
 * <li>Choose the projection</li>
 * <li>Define the number of calibration point</li>
 * <li>Define the calibration points</li>
 * </ul>
 * <li>Change map properties</li>
 * <ul>
 * <li>Change the map name</li>
 * <li>Delete the map</li>
 * </ul>
 * </ul>
 * The activity that holds this fragment must implement {@code MapCalibrationRequestListener}
 * interface, to respond to a calibration request for a given {@link Map}.
 *
 * @author peterLaurence on 16/04/16.
 */
public class MapSettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_MAP_ID = "mapId";
    private static final int IMAGE_REQUEST_CODE = 1338;
    private Map mMap;
    private MapListViewModel mapListViewModel;
    private AlertDialog saveMapDialog;
    private static final int MAP_SAVE_CODE = 3465;

    /* Convenience method */
    private static void setListPreferenceSummaryAndValue(ListPreference preference, String value) {
        preference.setSummary(value);
        preference.setValue(value);
    }

    /* Convenience method */
    private static void setEditTextPreferenceSummaryAndValue(EditTextPreference preference, String value) {
        preference.setSummary(value);
        preference.setText(value);
    }

    /**
     * Get the {@link Map} name from arguments bundle and init a {@code WeakReference}
     * from it.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mapListViewModel = new ViewModelProvider(requireActivity()).get(MapListViewModel.class);

        Bundle args = getArguments();
        // TODO: convert to Kotlin and use Safe Args https://developer.android.com/guide/navigation/navigation-pass-data
        if (args != null) {
            int mapId = args.getInt(ARG_MAP_ID);
            setMap(mapId);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        /* The Preferences layout */
        addPreferencesFromResource(R.xml.calibration_settings);
    }

    /**
     * Set the {@link Map} for this {@code MapSettingsFragment}.
     * <p>
     * This calls {@code initComponents} method, so it must be called after the Preferences
     * layout has been set with e.g {@code addPreferencesFromResource}.
     * </p>
     *
     * @param mapId the id of the {@link Map}
     */
    public void setMap(int mapId) {
        Map map = MapLoader.INSTANCE.getMap(mapId);
        mMap = map;

        if (map != null) {
            /* Choice is made to have the preference file name equal to the map name */
            getPreferenceManager().setSharedPreferencesName(map.getName());

            initComponents();
        }
    }

    private void initComponents() {
        Preference changeImageButton = getPreferenceManager().findPreference(
                getString(R.string.preference_change_image_key));
        ListPreference mCalibrationListPreference = getPreferenceManager().findPreference(
                getString(R.string.preference_projection_key));
        ListPreference mCalibrationPointsNumberPreference = getPreferenceManager().findPreference(
                getString(R.string.preference_numpoints_key));
        EditTextPreference mapNamePreference = getPreferenceManager().findPreference(
                getString(R.string.preference_map_title_key));
        Preference calibrationButton = getPreferenceManager().findPreference(
                getString(R.string.preference_calibration_button_key));

        Preference saveButton = getPreferenceManager().findPreference(
                getString(R.string.preference_save_button_key));

        changeImageButton.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            /* Search for all documents available via installed storage providers */
            intent.setType("image/*");
            startActivityForResult(intent, IMAGE_REQUEST_CODE);
            return true;
        });

        /* Set the summaries and the values of preferences according to the Map object */
        if (mMap != null) {
            String projectionName;
            if ((projectionName = mMap.getProjectionName()) == null) {
                projectionName = getString(R.string.projection_none);
            }
            setListPreferenceSummaryAndValue(mCalibrationListPreference, projectionName);
            setListPreferenceSummaryAndValue(mCalibrationPointsNumberPreference,
                    String.valueOf(mMap.getCalibrationPointsNumber()));
            setEditTextPreferenceSummaryAndValue(mapNamePreference, mMap.getName());
        }

        calibrationButton.setOnPreferenceClickListener(preference -> {
            NavDirections direction = MapSettingsFragmentDirections.actionMapSettingsFragmentToMapCalibrationFragment();
            NavHostFragment.findNavController(this).navigate(direction);
            return true;
        });

        mCalibrationPointsNumberPreference.setOnPreferenceChangeListener(((preference, newValue) -> {
            if (mMap != null) {
                switch ((String) newValue) {
                    case "2":
                        mMap.setCalibrationMethod(MapLoader.CalibrationMethod.SIMPLE_2_POINTS);
                        break;
                    case "3":
                        mMap.setCalibrationMethod(MapLoader.CalibrationMethod.CALIBRATION_3_POINTS);
                        break;
                    case "4":
                        mMap.setCalibrationMethod(MapLoader.CalibrationMethod.CALIBRATION_4_POINTS);
                        break;
                    default:
                        mMap.setCalibrationMethod(MapLoader.CalibrationMethod.SIMPLE_2_POINTS);
                }
                return true;
            }
            return false;
        }));

        mapNamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                mMap.setName((String) newValue);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        mCalibrationListPreference.setOnPreferenceChangeListener((preference, projectionName) -> {
            try {
                /* If the projection is set to none */
                if (getString(R.string.projection_none).equals(projectionName)) {
                    mMap.setProjection(null);
                    return true;
                }

                if (MapLoader.INSTANCE.mutateMapProjection(mMap, (String) projectionName)) {
                    String saveOkMsg = getString(R.string.calibration_projection_saved_ok);
                    Toast toast = Toast.makeText(getContext(), saveOkMsg, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    // TODO : show some warning ("Wrong Projection name").
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        saveButton.setOnPreferenceClickListener(preference -> {
            Context context = getContext();
            if (context == null) return true;
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            String title = context.getString(R.string.archive_dialog_title);
            builder.setTitle(title)
                    .setMessage(R.string.archive_dialog_description)
                    .setPositiveButton(R.string.ok_dialog,
                            (dialog, whichButton) -> {
                                if (mapListViewModel != null) {
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                    startActivityForResult(intent, MAP_SAVE_CODE);
                                }
                            })
                    .setNegativeButton(R.string.cancel_dialog_string,
                            (dialog, whichButton) -> dialog.dismiss()
                    );

            saveMapDialog = builder.create();
            saveMapDialog.show();

            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

        if (saveMapDialog != null) saveMapDialog.dismiss();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* Check if the request code is the one we are interested in */
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();

                try {
                    mMap.setImage(uri, getContext().getContentResolver());
                    saveChanges();
                } catch (Exception e) {
                    // no-op
                }
            }
        }

        /* After the user selected a folder in which to save a map, create an OutputStream using
         * the Storage Access Framework, and call the relevant view-model */
        if (requestCode == MAP_SAVE_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) return;
            Uri uri = data.getData();
            if (uri == null) return;
            Context context = getContext();
            if (context == null) return;
            DocumentFile docFile = DocumentFile.fromTreeUri(context, uri);
            if (docFile == null) return;
            if (docFile.isDirectory()) {
                if (mMap == null) return;
                String newFileName = mMap.generateNewNameWithDate() + ".zip";
                DocumentFile newFile = docFile.createFile("application/zip", newFileName);
                if (newFile == null) return;
                Uri uriZip = newFile.getUri();
                try {
                    OutputStream out = context.getContentResolver().openOutputStream(uriZip);
                    mapListViewModel.startZipTask(mMap.getId(), out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(sharedPreferences.getString(key, "default"));

            saveChanges();
        }
    }

    /**
     * Save the Map content
     */
    private void saveChanges() {
        if (mMap != null) {
            MapLoader.INSTANCE.saveMap(mMap);
        }
    }

    /**
     * The dialog that shows up when the user press the delete button
     */
    public static class ConfirmDeleteFragment extends DialogFragment {
        private WeakReference<Map> mMapWeakReference;
        private MapLoader.MapDeletedListener mDeleteMapListener;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.map_delete_question)
                    .setPositiveButton(R.string.delete_dialog, (dialog, id) -> {
                        /* Delete the map */
                        // TODO: refactor (this is really not the responsibility of a view to do this)
                        if (mMapWeakReference != null) {
                            Map map = mMapWeakReference.get();
                            if (map != null) {
                                MapLoader.INSTANCE.deleteMap(map, mDeleteMapListener);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel_dialog_string, (dialog, id) -> {
                        // Do nothing. This empty listener is used just to create the Cancel button.
                    });

            return builder.create();
        }

        public void setMapWeakRef(WeakReference<Map> mapWr) {
            mMapWeakReference = mapWr;
        }

        public void setDeleteMapListener(MapLoader.MapDeletedListener listener) {
            mDeleteMapListener = listener;
        }
    }
}
