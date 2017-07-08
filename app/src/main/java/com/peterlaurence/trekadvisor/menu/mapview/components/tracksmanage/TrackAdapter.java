package com.peterlaurence.trekadvisor.menu.mapview.components.tracksmanage;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link RouteGson.Route}).
 *
 * @author peterLaurence on 01/03/17.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<RouteGson.Route> mRouteList;
    private TrackSelectionListener mTrackSelectionListener;

    TrackAdapter(Map map, TrackSelectionListener trackSelectionListener) {
        mRouteList = map.getRoutes();
        mTrackSelectionListener = trackSelectionListener;
    }

    public void removeItem(int position) {
        mRouteList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.track_card, parent, false);

        return new TrackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(TrackViewHolder holder, int position) {
        RouteGson.Route route = mRouteList.get(position);
        holder.trackName.setText(route.name);
        holder.setVisibleButtonIcon(route.visible);
        holder.visibleButton.setOnClickListener(new VisibilityButtonClickListener(holder, this));
    }

    @Override
    public int getItemCount() {
        return mRouteList == null ? 0 : mRouteList.size();
    }

    interface TrackSelectionListener {
        void onVisibilityToggle(RouteGson.Route route);
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView trackName;
        ImageButton visibleButton;

        TrackViewHolder(View itemView) {
            super(itemView);
            cardView = (CardView) itemView.findViewById(R.id.cv_track);
            trackName = (TextView) itemView.findViewById(R.id.track_name);
            visibleButton = (ImageButton) itemView.findViewById(R.id.track_visible_btn);
        }

        void setVisibleButtonIcon(boolean visible) {
            visibleButton.setImageResource(visible ? R.drawable.ic_visibility_black_24dp :
                    R.drawable.ic_visibility_off_black_24dp);
        }
    }

    private static class VisibilityButtonClickListener implements View.OnClickListener {
        WeakReference<TrackViewHolder> mTrackViewHolderWeakReference;
        WeakReference<TrackAdapter> mTrackAdapterWeakReference;

        VisibilityButtonClickListener(TrackViewHolder trackViewHolder, TrackAdapter trackAdapter) {
            mTrackViewHolderWeakReference = new WeakReference<>(trackViewHolder);
            mTrackAdapterWeakReference = new WeakReference<>(trackAdapter);
        }

        @Override
        public void onClick(View v) {
            TrackViewHolder trackViewHolder = mTrackViewHolderWeakReference.get();
            TrackAdapter trackAdapter = mTrackAdapterWeakReference.get();

            if (trackViewHolder != null && trackAdapter != null) {
                int position = trackViewHolder.getAdapterPosition();
                RouteGson.Route route = trackAdapter.mRouteList.get(position);
                route.toggleVisibility();
                trackViewHolder.setVisibleButtonIcon(route.visible);

                trackAdapter.mTrackSelectionListener.onVisibilityToggle(route);
            }
        }
    }
}
