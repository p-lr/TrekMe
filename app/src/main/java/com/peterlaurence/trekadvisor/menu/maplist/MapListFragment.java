package com.peterlaurence.trekadvisor.menu.maplist;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapLoader;

/**
 * A {@link Fragment} subclass that displays the list of available maps, using a {@link RecyclerView}.
 * <p>
 *     Activities that contain this fragment must implement the
 *     {@link MapListFragment.OnMapListFragmentInteractionListener} interface to handle interaction
 *     events.
 * </p>
 */
public class MapListFragment extends Fragment implements
        MapAdapter.MapSelectionListener,
        MapAdapter.MapSettingsListener,
        MapLoader.MapListUpdateListener {
    private static final String RECREATE_FRAGMENT_KEY = "RECREATE_FRAGMENT_KEY";

    private FrameLayout rootView;
    private RecyclerView recyclerView;

    private OnMapListFragmentInteractionListener mListener;

    private Map mCurrentMap;      // The map selected by the user in the list
    private Map mSettingsMap;  // The map that the user wants to calibrate

    public MapListFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of this fragment using the provided parameters.
     * Takes no arguments for instance, so this methods is quite useless, but let it because it is a
     * better practice to create fragments from a factory rather directly from constructor.
     */
    public static MapListFragment newInstance() {
        return new MapListFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMapListFragmentInteractionListener) {
            mListener = (OnMapListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapListFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.get(RECREATE_FRAGMENT_KEY) != null &&
                !((boolean) savedInstanceState.get(RECREATE_FRAGMENT_KEY)) &&
                rootView != null) {
            return rootView;
        }

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_list, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (recyclerView != null) {
            return;
        }

        generateMapList();
    }

    /**
     * Get a reference to the last {@link Map} that has been selected.
     */
    public Map getCurrentMap() {
        return mCurrentMap;
    }

    /**
     * Get a reference to the last {@link Map} that the user selected to edit (with the
     * settings button).
     */
    public Map getSettingsMap() {
        return mSettingsMap;
    }

    private void generateMapList() {
        recyclerView = new RecyclerView(this.getContext());
        recyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(llm);

        MapAdapter adapter = new MapAdapter(null, this, this);

        /**
         * The {@link MapAdapter} and this fragment are interested by the map list update event.
         */
        MapLoader.getInstance().addMapListUpdateListener(adapter);
        MapLoader.getInstance().addMapListUpdateListener(this);
        MapLoader.getInstance().generateMaps();
        recyclerView.setAdapter(adapter);

        rootView.addView(recyclerView, 0);
    }

    @Override
    public void onMapSelected(Map map) {
        mCurrentMap = map;
        if (mListener != null) {
            mListener.onMapSelectedFragmentInteraction(map);
        }
    }

    @Override
    public void onMapListUpdate(boolean mapsFound) {
        rootView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);

        /* If no maps found, display a warning */
        if (!mapsFound) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setMessage(getString(R.string.no_maps_found_warning))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok_dialog), null);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapSettings(Map map) {
        mSettingsMap = map;
        if (mListener != null) {
            mListener.onMapSettingsFragmentInteraction(map);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnMapListFragmentInteractionListener {
        void onMapSelectedFragmentInteraction(Map map);
        void onMapSettingsFragmentInteraction(Map map);
    }

    /**
     * Don't recreate the fragment on a configuration change. Can be easily parametrized if
     * needed.
     * <p/>
     * Important note : if no data is appended to the {@link Bundle outState} the
     * {@code Fragment.onCreateView} will receive a null {@link Bundle savedInstanceState}.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(RECREATE_FRAGMENT_KEY, false);
    }
}
