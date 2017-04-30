package com.peterlaurence.trekadvisor.menu.mapimport;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;

/**
 * A {@link Fragment} subclass that displays the list of maps archives available for import.
 *
 * @author peterLaurence on 08/06/16.
 */
public class MapImportFragment extends Fragment implements MapLoader.MapArchiveListUpdateListener {

    private FrameLayout rootView;
    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_map_import, container, false);
        generateMapList();
        return rootView;
    }

    private void generateMapList() {
        mRecyclerView = new RecyclerView(this.getContext());
        mRecyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        mRecyclerView.setLayoutManager(llm);

        MapArchiveAdapter mapArchiveAdapter = new MapArchiveAdapter(null);
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
}
