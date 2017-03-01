package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;

/**
 * A {@link Fragment} subclass that shows the tracks currently available for a given map, and
 * provides the ability to import new tracks.
 *
 * @author peterLaurence on 01/03/17.
 */
public class TracksManageFragment extends Fragment {
    private FrameLayout rootView;
    private RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        rootView = (FrameLayout) inflater.inflate(R.layout.fragment_tracks_manage, container, false);
        return rootView;
    }


    public void generateTracks(Map map) {
        mRecyclerView = new RecyclerView(this.getContext());
        mRecyclerView.setHasFixedSize(false);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        mRecyclerView.setLayoutManager(llm);

        TrackAdapter trackAdapter = new TrackAdapter(map);
        mRecyclerView.setAdapter(trackAdapter);

        rootView.addView(mRecyclerView, 0);
    }
}
