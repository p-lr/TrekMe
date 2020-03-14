package com.peterlaurence.trekme.ui.maplist;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.map.maploader.MapLoader;
import com.peterlaurence.trekme.model.map.MapModel;
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.os.Build.VERSION_CODES.Q;

/**
 * A {@link Fragment} that displays the list of available maps, using a {@link RecyclerView}.
 * <p>
 * Activities that contain this fragment must implement the
 * {@link MapListFragment.OnMapListFragmentInteractionListener} interface to handle interaction
 * events.
 * </p>
 */
public class MapListFragment extends Fragment implements
        MapAdapter.MapSelectionListener,
        MapAdapter.MapSettingsListener,
        MapAdapter.MapDeleteListener,
        MapLoader.MapDeletedListener {

    private FrameLayout rootView;
    private LinearLayoutManager llm;
    private RecyclerView recyclerView;
    private MapAdapter adapter;
    private static final String llmStateKey = "llmState";

    private MapListViewModel viewModel;
    private OnMapListFragmentInteractionListener mListener;

    public MapListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
        setHasOptionsMenu(true);

        viewModel = new ViewModelProvider(requireActivity()).get(MapListViewModel.class);
        viewModel.getMaps().observe(this, maps -> {
            if (maps != null) {
                /* Set data */
                onMapListUpdate(maps);

                /* Restore the recyclerView state if the device was rotated */
                Parcelable llmState;
                if (savedInstanceState != null) {
                    llmState = savedInstanceState.getParcelable(llmStateKey);
                    if (llm != null) {
                        llm.onRestoreInstanceState(llmState);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_list, container, false);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        /* When modifications happened outside of the context of this fragment, e.g if a map image
         * was changed in the settings fragment, we need to refresh the view. */
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (recyclerView != null) {
            return;
        }

        generateMapList();
    }

    private void generateMapList() {
        Context ctx = getContext();
        if (ctx != null) {
            recyclerView = new RecyclerView(ctx);
            recyclerView.setHasFixedSize(false);

            llm = new LinearLayoutManager(ctx);
            recyclerView.setLayoutManager(llm);

            adapter = new MapAdapter(null, this, this, this,
                    ctx.getColor(R.color.colorAccent),
                    ctx.getColor(R.color.colorPrimaryTextWhite),
                    ctx.getColor(R.color.colorPrimaryTextBlack));

            recyclerView.setAdapter(adapter);

            rootView.addView(recyclerView, 0);
        }
    }

    @Override
    public void onMapSelected(Map map) {
        viewModel.setMap(map);
        if (mListener != null) {
            mListener.onMapSelectedFragmentInteraction(map);
        }
    }

    /**
     * This fragment and its {@link MapAdapter} need to take action on map list update.
     */
    private void onMapListUpdate(List<Map> mapList) {
        rootView.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        adapter.onMapListUpdate(mapList);

        /* If no maps found, suggest to navigate to map creation */
        if (mapList.size() == 0) {
            rootView.findViewById(R.id.emptyMapPanel).setVisibility(View.VISIBLE);
            Button btn = rootView.findViewById(R.id.button_go_to_map_create);
            btn.setOnClickListener((e) -> mListener.onGoToMapCreation());

            /* Specifically for Android 10, temporarily explain why the list of map is empty. */
            if (android.os.Build.VERSION.SDK_INT == Q) {
                rootView.findViewById(R.id.android10_warning).setVisibility(View.VISIBLE);
            }
        } else {
            rootView.findViewById(R.id.emptyMapPanel).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapSettings(Map map) {
        MapModel.INSTANCE.setSettingsMap(map);
        if (mListener != null) {
            mListener.onMapSettingsFragmentInteraction(map);
        }
    }

    @Override
    public void onMapDelete(Map map) {
        MapSettingsFragment.ConfirmDeleteFragment f = new MapSettingsFragment.ConfirmDeleteFragment();
        f.setMapWeakRef(new WeakReference<>(map));
        f.setDeleteMapListener(this);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            f.show(fragmentManager, "delete");
        }
    }

    @Override
    public void onMapDeleted() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Parcelable llmState = llm.onSaveInstanceState();
        outState.putParcelable(llmStateKey, llmState);
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

        void onDefaultMapDownloaded();

        void onGoToMapCreation();
    }
}
