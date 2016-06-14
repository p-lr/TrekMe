package com.peterlaurence.trekadvisor.menu.maplist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapLoader;

import java.lang.ref.WeakReference;

/**
 * Fragment that shows the settings for a given map. It provides the abilities to :
 * <ul>
 * <li>Calibrate the map</li>
 * <li>Change the map name</li>
 * </ul>
 * The activity that holds this fragment must implement {@code MapCalibrationRequestListener}
 * interface, to respond to a calibration request for a given {@link Map}.
 *
 * @author peterLaurence on 16/04/16.
 */
public class MapSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_MAP_NAME = "arg_map_name";
    private WeakReference<Map> mMapWeakReference;

    private CalibrationButtonClickListener mCalibrationButtonClickListener;
    private MapCalibrationRequestListener mMapCalibrationRequestListener;

    private LinearLayout rootView;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface MapCalibrationRequestListener {
        void onMapCalibrationRequest(Map map);
    }

    private class CalibrationButtonClickListener implements Preference.OnPreferenceClickListener{
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Map map = mMapWeakReference.get();
            if (map != null) {
                mMapCalibrationRequestListener.onMapCalibrationRequest(map);
            }
            return true;
        }
    }

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param mapName the name of the {@link Map}
     * @return A new instance of {@code MapSettingsFragment}
     */
    public static MapSettingsFragment newInstance(String mapName) {
        MapSettingsFragment fragment = new MapSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MAP_NAME, mapName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        /**
         * Get the {@link Map} name from arguments bundle and init a {@code WeakReference}
         * from it.
         */
        Bundle args = getArguments();
        String mapName = "";
        if (args != null) {
            mapName = args.getString(ARG_MAP_NAME);
        }

        /* The click listener for the calibration button */
        mCalibrationButtonClickListener = new CalibrationButtonClickListener();

        /* The Preferences layout */
        addPreferencesFromResource(R.xml.calibration_settings);

        setMap(mapName);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (savedInstanceState != null && rootView != null) {
            return rootView;
        }

        rootView = (LinearLayout) inflater.inflate(R.layout.fragment_map_settings, container, false);
        return rootView;
    }

    /**
     * Set the {@link Map} for this {@code MapSettingsFragment}.
     * <p>
     *     This calls {@code initComponents} method, so it must be called after the Preferences
     *     layout has been set with e.g {@code addPreferencesFromResource}.
     * </p>
     *
     * @param mapName the name of the {@link Map}
     */
    public void setMap(String mapName) {
        mMapWeakReference = new WeakReference<>(MapLoader.getInstance().getMap(mapName));

        /* Choice is made to have the preference file name equal to the map name */
        getPreferenceManager().setSharedPreferencesName(mapName);

        initComponents();
    }

    private void initComponents() {
        ListPreference mCalibrationListPreference = (ListPreference) getPreferenceManager().findPreference(
                getString(R.string.preference_projection_key));
        ListPreference mCalibrationPointsNumberPreference = (ListPreference) getPreferenceManager().findPreference(
                getString(R.string.preference_numpoints_key));
        EditTextPreference mapNamePreference = (EditTextPreference) getPreferenceManager().findPreference(
                getString(R.string.preference_map_title_key));
        Preference calibrationButton = getPreferenceManager().findPreference(
                getString(R.string.preference_calibration_button_key));

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

        calibrationButton.setOnPreferenceClickListener(mCalibrationButtonClickListener);

        mapNamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                try {
                    mMapWeakReference.get().setName((String) newValue);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        mCalibrationListPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object projectionName) {
                try {
                    /* If the projection is set to none */
                    if (getString(R.string.projection_none).equals(projectionName)) {
                        mMapWeakReference.get().setProjection(null);
                        return true;
                    }

                    if (MapLoader.getInstance().mutateMapProjection(mMapWeakReference.get(), (String) projectionName)) {
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
            }
        });
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(sharedPreferences.getString(key, "default"));

            /* Save the Map content */
            Map map = mMapWeakReference.get();
            MapLoader.getInstance().saveMap(map);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MapCalibrationRequestListener) {
            mMapCalibrationRequestListener = (MapCalibrationRequestListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    "must implement MapCalibrationRequestListener");
        }
    }
}
