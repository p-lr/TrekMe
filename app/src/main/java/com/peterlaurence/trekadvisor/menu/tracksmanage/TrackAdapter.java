package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link MapGson.Track}).
 *
 * @author peterLaurence on 01/03/17.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<MapGson.Track> mTrackList;

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;

        public TrackViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_track);

        }
    }

    TrackAdapter(Map map) {
        mTrackList = map.getMapGson().tracks;
    }

    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
