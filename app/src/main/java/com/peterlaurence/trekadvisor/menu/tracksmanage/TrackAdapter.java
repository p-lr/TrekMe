package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapLoader;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link MapGson.Track}).
 *
 * @author peterLaurence on 01/03/17.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<MapGson.Track> mTrackList;
    private Map mMap;

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView trackName;
        CheckBox checkBox;

        public TrackViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_track);
            trackName = (TextView) itemView.findViewById(R.id.track_name);
            checkBox = (CheckBox) itemView.findViewById(R.id.track_checkbox);
        }
    }

    TrackAdapter(Map map) {
        mMap = map;
        mTrackList = map.getMapGson().tracks;
    }

    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {
        final MapGson.Track track = mTrackList.get(position);
        holder.trackName.setText(track.name);
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                track.setVisibility(isChecked);
                MapLoader.getInstance().saveMap(mMap);
            }
        });
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
