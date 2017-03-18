package com.peterlaurence.trekadvisor.menu.tracksmanage;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.MapLoader;
import com.peterlaurence.trekadvisor.core.map.gson.MapGson;

import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link MapGson.Route}).
 *
 * @author peterLaurence on 01/03/17.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<MapGson.Route> mRouteList;
    private Map mMap;

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView trackName;
        ImageButton visibleButton;

        public TrackViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_track);
            trackName = (TextView) itemView.findViewById(R.id.track_name);
            visibleButton = (ImageButton) itemView.findViewById(R.id.track_visible_btn);
        }
    }

    TrackAdapter(Map map) {
        mMap = map;
        mRouteList = map.getMapGson().routes;
    }

    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.track_card, parent, false);

        return new TrackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {
        final MapGson.Route route = mRouteList.get(position);
        holder.trackName.setText(route.name);
//        holder.visibleButton.setChecked(route.visible);
//        holder.visibleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                route.setVisibility(isChecked);
//                MapLoader.getInstance().saveMap(mMap);
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return mRouteList == null ? 0 : mRouteList.size();
    }
}
