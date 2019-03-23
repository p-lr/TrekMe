package com.peterlaurence.trekme.ui.maplist;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.ui.maplist.dialogs.ArchiveMapDialog;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

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

    private static final String ARG_MAP_ID = "arg_map_id";
    private static final int IMAGE_REQUEST_CODE = 1338;
    private WeakReference<Map> mMapWeakReference;

    private MapCalibrationRequestListener mMapCalibrationRequestListener;

    /**
     * Factory method to create a new instance of this fragment. <br>
     * Arguments supplied here will be retained across fragment destroy and
     * creation.
     *
     * @param mapId the id of the {@link Map}
     * @return A new instance of {@code MapSettingsFragment}
     */
    public static MapSettingsFragment newInstance(int mapId) {
        MapSettingsFragment fragment = new MapSettingsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MAP_ID, mapId);
        fragment.setArguments(args);
        return fragment;
    }

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

        Bundle args = getArguments();
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
        mMapWeakReference = new WeakReference<>(map);

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
        final Map map = mMapWeakReference.get();
        if (map != null) {
            String projectionName;
            if ((projectionName = map.getProjectionName()) == null) {
                projectionName = getString(R.string.projection_none);
            }
            setListPreferenceSummaryAndValue(mCalibrationListPreference, projectionName);
            setListPreferenceSummaryAndValue(mCalibrationPointsNumberPreference,
                    String.valueOf(map.getCalibrationPointsNumber()));
            setEditTextPreferenceSummaryAndValue(mapNamePreference, map.getName());
        }

        calibrationButton.setOnPreferenceClickListener(preference -> {
            mMapCalibrationRequestListener.onMapCalibrationRequest();
            return true;
        });

        mCalibrationPointsNumberPreference.setOnPreferenceChangeListener(((preference, newValue) -> {
            Map map_ = mMapWeakReference.get();
            if (map_ != null) {
                switch ((String) newValue) {
                    case "2":
                        map_.setCalibrationMethod(MapLoader.CALIBRATION_METHOD.SIMPLE_2_POINTS);
                        break;
                    case "3":
                        map_.setCalibrationMethod(MapLoader.CALIBRATION_METHOD.CALIBRATION_3_POINTS);
                        break;
                    case "4":
                        map_.setCalibrationMethod(MapLoader.CALIBRATION_METHOD.CALIBRATION_4_POINTS);
                        break;
                    default:
                        map_.setCalibrationMethod(MapLoader.CALIBRATION_METHOD.SIMPLE_2_POINTS);
                }
                return true;
            }
            return false;
        }));

        mapNamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                mMapWeakReference.get().setName((String) newValue);
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        mCalibrationListPreference.setOnPreferenceChangeListener((preference, projectionName) -> {
            try {
                /* If the projection is set to none */
                if (getString(R.string.projection_none).equals(projectionName)) {
                    mMapWeakReference.get().setProjection(null);
                    return true;
                }

                if (MapLoader.INSTANCE.mutateMapProjection(mMapWeakReference.get(), (String) projectionName)) {
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
            ArchiveMapDialog archiveMapDialog = ArchiveMapDialog.newInstance(map.getId());
            archiveMapDialog.show(getFragmentManager(), "ArchiveMapDialog");
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
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        /* Check if the request code is the one we are interested in */
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();

                try {
                    mMapWeakReference.get().setImage(uri, getContext().getContentResolver());
                    saveChanges();
                } catch (Exception e) {
                    // no-op
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

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MapCalibrationRequestListener) {
            mMapCalibrationRequestListener = (MapCalibrationRequestListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    "must implement MapCalibrationRequestListener");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface MapCalibrationRequestListener {
        void onMapCalibrationRequest();
    }

    /**
     * Save the Map content
     */
    private void saveChanges() {
        Map map = mMapWeakReference.get();
        MapLoader.INSTANCE.saveMap(map);
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
