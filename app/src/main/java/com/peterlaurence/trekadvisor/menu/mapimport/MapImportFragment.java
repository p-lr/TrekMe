package com.peterlaurence.trekadvisor.menu.mapimport;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;

/**
 * A {@link Fragment} subclass that displays the list of maps archives available for import.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapImportFragment extends Fragment implements MapLoader.MapArchiveListUpdateListener,
        MapImporter.MapImportListener {

    private FrameLayout rootView;
    private RecyclerView mRecyclerView;
    private OnMapArchiveFragmentInteractionListener mListener;
    private View mView;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnMapArchiveFragmentInteractionListener) {
            mListener = (OnMapArchiveFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMapArchiveFragmentInteractionListener");
        }

        MapImporter.addMapImportListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_import, container, false);
        generateMapList();
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mView = view;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        MapImporter.clearMapImportListenerList();
    }

    private void generateMapList() {
        mRecyclerView = new RecyclerView(getContext());
        mRecyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(llm);

        MapArchiveAdapter mapArchiveAdapter = new MapArchiveAdapter();
        MapLoader.getInstance().addMapArchiveListUpdateListener(this);
        MapLoader.getInstance().addMapArchiveListUpdateListener(mapArchiveAdapter);
        MapLoader.getInstance().generateMapArchives();
        mRecyclerView.setAdapter(mapArchiveAdapter);

        rootView.addView(mRecyclerView, 0);
    }

    @Override
    public void onMapArchiveListUpdate() {
        rootView.findViewById(R.id.import_main_panel).setVisibility(View.GONE);
    }

    @Override
    public void onMapImported(Map map, MapImporter.MapParserStatus status) {
        Snackbar snackbar = Snackbar.make(mView, R.string.snack_msg_show_map_list, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.ok_dialog, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onMapArchiveFragmentInteraction();
            }
        });
        snackbar.show();
    }

    @Override
    public void onMapImportError(MapImporter.MapParseException e) {

    }

    public interface OnMapArchiveFragmentInteractionListener {
        void onMapArchiveFragmentInteraction();
    }
}
