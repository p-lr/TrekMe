package com.peterlaurence.trekme.ui.maplist;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.RecyclerView;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.Map;

import java.util.List;

/**
 * Adapter to provide access to the data set (here a list of {@link Map}).
 * <p/>
 *
 * @author peterLaurence on 26/12/15.
 */
public class MapAdapter extends RecyclerView.Adapter<MapAdapter.MapViewHolder> {
    private List<Map> maps;
    private MapSelectionListener mMapSelectionListener;
    private MapSettingsListener mMapSettingsListener;
    private MapDeleteListener mMapDeleteListener;
    private MapFavoriteListener mMapFavoriteListener;

    private int selectedMapIndex = -1;
    private int previousSelectedMapIndex = -1;

    private int mColorAccent;
    private int mColorWhiteText;
    private int mColorBlackText;
    private Resources mResources;

    MapAdapter(@Nullable List<Map> maps, MapSelectionListener mapSelectionListener,
               MapSettingsListener mapSettingsListener, MapDeleteListener mapDeleteListener,
               MapFavoriteListener mapFavoriteListener,
               int accentColor, int whiteTextColor,
               int blackTextColor, Resources resources) {
        this.maps = maps;
        mMapSelectionListener = mapSelectionListener;
        mMapSettingsListener = mapSettingsListener;
        mMapDeleteListener = mapDeleteListener;
        mMapFavoriteListener = mapFavoriteListener;

        mColorAccent = accentColor;
        mColorWhiteText = whiteTextColor;
        mColorBlackText = blackTextColor;
        mResources = resources;
    }

    void setMapList(List<Map> mapList) {
        maps = mapList;
    }

    /**
     * Simple implementation of a toggle selection. When an item is clicked, we change its
     * background and we remember his index. When another item is clicked, the background of the
     * first item is set to its original state.
     *
     * @param position index of the selected view
     */
    private void updateSelectionColor(int position) {
        selectedMapIndex = position;
        notifyItemChanged(position);
        if (previousSelectedMapIndex != -1) {
            notifyItemChanged(previousSelectedMapIndex);
        }
        previousSelectedMapIndex = position;
    }

    @Override
    @NonNull
    public MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context ctx = parent.getContext();
        View v = LayoutInflater.from(ctx).inflate(R.layout.map_row, parent, false);

        return new MapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MapViewHolder holder, int position) {
        final Map map = maps.get(position);
        RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(mResources, map.getImage());
        dr.setCornerRadius(16f);
        holder.mapImage.setImageDrawable(dr);
        holder.mapName.setText(map.getName());
        holder.calibrationStatus.setText(map.getDescription());
        if (map.isFavorite()) {
            holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_24);
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24);
        }

        if (holder.getLayoutPosition() == selectedMapIndex) {
            holder.cardView.setCardBackgroundColor(mColorAccent);
            holder.mapName.setTextColor(mColorWhiteText);
            holder.editButton.setTextColor(mColorWhiteText);
            holder.deleteButton.setColorFilter(mColorWhiteText);
            holder.calibrationStatus.setTextColor(mColorWhiteText);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.mapName.setTextColor(mColorBlackText);
            holder.editButton.setTextColor(mColorAccent);
            holder.deleteButton.setColorFilter(mColorAccent);
        }
        switch (map.getCalibrationStatus()) {
            case OK:
                holder.calibrationStatus.setText(R.string.calibration_status_ok);
                break;
            case NONE:
                holder.calibrationStatus.setText(R.string.calibration_status_none);
                break;
            case ERROR:
                holder.calibrationStatus.setText(R.string.calibration_status_error);
                break;
        }

        /* Set click listeners */
        holder.itemView.setOnClickListener(v -> {
            // Toggle background color
            updateSelectionColor(position);

            // Call the listener for Map selection
            mMapSelectionListener.onMapSelected(map);
        });
        holder.editButton.setOnClickListener(v -> mMapSettingsListener.onMapSettings(map));
        holder.deleteButton.setOnClickListener(v -> mMapDeleteListener.onMapDelete(map));
        holder.favoriteButton.setOnClickListener(v -> {
                    /* Toggle icon */
                    if (map.isFavorite()) {
                        holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24);
                    } else {
                        holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_24);
                    }
                    mMapFavoriteListener.onMapFavorite(map, position);
                }
        );
    }

    @Override
    public long getItemId(int position) {
        return (long) maps.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return maps == null ? 0 : maps.size();
    }

    /**
     * When an item gets selected, the {@link MapSelectionListener} is called with the corresponding
     * {@link Map}.
     */
    public interface MapSelectionListener {
        void onMapSelected(Map m);
    }

    /**
     * When the settings button of an item is clicked, the {@link MapSettingsListener} is called
     * with the corresponding {@link Map}.
     */
    public interface MapSettingsListener {
        void onMapSettings(Map m);
    }

    /**
     * When the deletion of a {@link Map} is confirmed, the {@link MapDeleteListener} is called with the
     * corresponding {@link Map}.
     */
    public interface MapDeleteListener {
        void onMapDelete(Map map);
    }

    /**
     * When a {@link Map} is set (or unset) as favorite, this listener is invoked with the
     * corresponding {@link Map}.
     */
    public interface MapFavoriteListener {
        void onMapFavorite(@NonNull Map map, int formerPosition);
    }

    /**
     * The view for each {@link Map}
     */
    public static class MapViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView mapName;
        TextView calibrationStatus;
        ImageView mapImage;
        Button editButton;
        ImageButton deleteButton;
        ImageButton favoriteButton;

        public MapViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv);
            mapName = itemView.findViewById(R.id.map_name);
            calibrationStatus = itemView.findViewById(R.id.map_calibration_status);
            mapImage = itemView.findViewById(R.id.map_preview_image);
            editButton = itemView.findViewById(R.id.map_manage_btn);
            deleteButton = itemView.findViewById(R.id.map_delete_btn);
            favoriteButton = itemView.findViewById(R.id.map_favorite_btn);
        }
    }
}
