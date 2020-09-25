package com.peterlaurence.trekme.ui.mapview.components.tracksmanage;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.gson.RouteGson;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link RouteGson.Route}).
 *
 * @author peterLaurence on 01/03/17.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private TrackSelectionListener mTrackSelectionListener;

    private int mSelectedRouteIndex = -1;
    private int mPreviousSelectedRouteIndex = -1;

    private int mColorAccent;
    private int mColorWhite;
    private int mColorBlack;
    private DiffUtil.ItemCallback<RouteGson.Route> diffCallback = new DiffUtil.ItemCallback<RouteGson.Route>() {
        @Override
        public boolean areItemsTheSame(@NonNull RouteGson.Route oldItem, @NonNull RouteGson.Route newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RouteGson.Route oldItem, @NonNull RouteGson.Route newItem) {
            return oldItem.equals(newItem);
        }
    };
    private AsyncListDiffer<RouteGson.Route> differ = new AsyncListDiffer<>(this, diffCallback);

    TrackAdapter(TrackSelectionListener trackSelectionListener, int accentColor,
                 int whiteTextColor, int blackTextColor) {
        mTrackSelectionListener = trackSelectionListener;
        mColorAccent = accentColor;
        mColorWhite = whiteTextColor;
        mColorBlack = blackTextColor;
    }

    void setRouteList(List<RouteGson.Route> routeList) {
        differ.submitList(routeList);
    }

    public RouteGson.Route getRouteAt(int position) {
        try {
            return differ.getCurrentList().get(position);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public RouteGson.Route getSelectedRoute() {
        try {
            return differ.getCurrentList().get(mSelectedRouteIndex);
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    public void restoreSelectionIndex(int selectionIndex) {
        mSelectedRouteIndex = selectionIndex;
        updateSelectionColor(selectionIndex);
    }

    public int getSelectedRouteIndex() {
        return mSelectedRouteIndex;
    }

    /**
     * Simple implementation of a toggle selection. When an item is clicked, we change its
     * background and we remember his index. When another item is clicked, the background of the
     * first item is set to its original state.
     *
     * @param position index of the selected view
     */
    private void updateSelectionColor(int position) {
        mSelectedRouteIndex = position;
        notifyItemChanged(position);
        if (mPreviousSelectedRouteIndex != -1) {
            notifyItemChanged(mPreviousSelectedRouteIndex);
        }
        mPreviousSelectedRouteIndex = position;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.track_card, parent, false);

        return new TrackViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        RouteGson.Route route = differ.getCurrentList().get(position);
        if (route == null) return;
        holder.trackName.setText(route.name);
        holder.setVisibleButtonIcon(route.visible);
        holder.visibleButton.setOnClickListener(new VisibilityButtonClickListener(holder, this));

        if (holder.getLayoutPosition() == mSelectedRouteIndex) {
            holder.cardView.setCardBackgroundColor(mColorAccent);
            holder.trackName.setTextColor(mColorWhite);
            holder.visibleButton.setColorFilter(mColorWhite);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.trackName.setTextColor(mColorBlack);
            holder.visibleButton.setColorFilter(mColorBlack);
        }

        holder.itemView.setOnClickListener(new TrackViewHolderClickListener(holder, this));
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    interface TrackSelectionListener {
        void onTrackSelected();

        void onVisibilityToggle(RouteGson.Route route);
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView trackName;
        ImageButton visibleButton;

        TrackViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv_track);
            trackName = itemView.findViewById(R.id.track_name);
            visibleButton = itemView.findViewById(R.id.track_visible_btn);
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
                RouteGson.Route route = trackAdapter.getRouteAt(position);
                route.toggleVisibility();
                trackViewHolder.setVisibleButtonIcon(route.visible);

                trackAdapter.mTrackSelectionListener.onVisibilityToggle(route);
            }
        }
    }

    private static class TrackViewHolderClickListener implements View.OnClickListener {
        WeakReference<TrackViewHolder> mTrackViewHolderWeakReference;
        WeakReference<TrackAdapter> mTrackAdapterWeakReference;

        TrackViewHolderClickListener(TrackViewHolder holder, TrackAdapter adapter) {
            mTrackViewHolderWeakReference = new WeakReference<>(holder);
            mTrackAdapterWeakReference = new WeakReference<>(adapter);
        }

        @Override
        public void onClick(View v) {
            if (mTrackAdapterWeakReference != null && mTrackViewHolderWeakReference != null) {
                TrackViewHolder holder = mTrackViewHolderWeakReference.get();
                TrackAdapter adapter = mTrackAdapterWeakReference.get();
                if (adapter != null && holder != null) {
                    int position = holder.getAdapterPosition();

                    /* Toggle background color */
                    adapter.updateSelectionColor(position);

                    /* Call the listener for track selection */
                    adapter.mTrackSelectionListener.onTrackSelected();
                }
            }
        }
    }
}
