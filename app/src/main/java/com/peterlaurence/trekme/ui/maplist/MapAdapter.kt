package com.peterlaurence.trekme.ui.maplist

import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.Map.CalibrationStatus
import com.peterlaurence.trekme.ui.maplist.MapAdapter.MapViewHolder

/**
 * Adapter to provide access to the data set (here a list of [Map]).
 *
 * @author peterLaurence on 26/12/15 - Converted to Kotlin on 02/07/2020
 */
class MapAdapter internal constructor(private var maps: List<Map>?,
                                      private val mapSelectionListener: MapSelectionListener,
                                      private val mapSettingsListener: MapSettingsListener,
                                      private val mapDeleteListener: MapDeleteListener,
                                      private val mapFavoriteListener: MapFavoriteListener,
                                      private val colorAccent: Int, private val colorWhiteText: Int,
                                      private val colorBlackText: Int, private val resources: Resources) : RecyclerView.Adapter<MapViewHolder>() {
    private var selectedMapIndex = -1
    private var previousSelectedMapIndex = -1

    fun setMapList(mapList: List<Map>?) {
        maps = mapList
    }

    /**
     * Simple implementation of a toggle selection. When an item is clicked, we change its
     * background and we remember his index. When another item is clicked, the background of the
     * first item is set to its original state.
     *
     * @param position index of the selected view
     */
    private fun updateSelectionColor(position: Int) {
        selectedMapIndex = position
        notifyItemChanged(position)
        if (previousSelectedMapIndex != -1) {
            notifyItemChanged(previousSelectedMapIndex)
        }
        previousSelectedMapIndex = position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.map_row, parent, false)
        return MapViewHolder(v)
    }

    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val map = maps?.get(position) ?: return
        val dr = RoundedBitmapDrawableFactory.create(resources, map.image)
        dr.cornerRadius = 16f
        holder.mapImage.setImageDrawable(dr)
        holder.mapName.text = map.name
        holder.calibrationStatus.text = map.description
        if (map.isFavorite) {
            holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_24)
        } else {
            holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24)
        }
        if (holder.layoutPosition == selectedMapIndex) {
            holder.cardView.setCardBackgroundColor(colorAccent)
            holder.mapName.setTextColor(colorWhiteText)
            holder.editButton.setTextColor(colorWhiteText)
            holder.deleteButton.setColorFilter(colorWhiteText)
            holder.calibrationStatus.setTextColor(colorWhiteText)
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.mapName.setTextColor(colorBlackText)
            holder.editButton.setTextColor(colorAccent)
            holder.deleteButton.setColorFilter(colorAccent)
        }
        when (map.calibrationStatus) {
            CalibrationStatus.OK -> holder.calibrationStatus.setText(R.string.calibration_status_ok)
            CalibrationStatus.NONE, null -> holder.calibrationStatus.setText(R.string.calibration_status_none)
            CalibrationStatus.ERROR -> holder.calibrationStatus.setText(R.string.calibration_status_error)
        }

        /* Set click listeners */
        holder.itemView.setOnClickListener {
            // Toggle background color
            updateSelectionColor(position)

            // Call the listener for Map selection
            mapSelectionListener.onMapSelected(map)
        }
        holder.editButton.setOnClickListener { mapSettingsListener.onMapSettings(map) }
        holder.deleteButton.setOnClickListener { mapDeleteListener.onMapDelete(map) }
        holder.favoriteButton.setOnClickListener {
            /* Toggle icon */
            if (map.isFavorite) {
                holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24)
            } else {
                holder.favoriteButton.setImageResource(R.drawable.ic_baseline_star_24)
            }
            mapFavoriteListener.onMapFavorite(map, position)
        }
    }

    override fun getItemId(position: Int): Long {
        return maps?.get(position)?.id?.toLong() ?: -1
    }

    override fun getItemCount(): Int {
        return maps?.size ?: 0
    }

    /**
     * When an item gets selected, the [MapSelectionListener] is called with the corresponding
     * [Map].
     */
    interface MapSelectionListener {
        fun onMapSelected(map: Map)
    }

    /**
     * When the settings button of an item is clicked, the [MapSettingsListener] is called
     * with the corresponding [Map].
     */
    interface MapSettingsListener {
        fun onMapSettings(map: Map)
    }

    /**
     * When the deletion of a [Map] is confirmed, the [MapDeleteListener] is called with the
     * corresponding [Map].
     */
    interface MapDeleteListener {
        fun onMapDelete(map: Map)
    }

    /**
     * When a [Map] is set (or unset) as favorite, this listener is invoked with the
     * corresponding [Map].
     */
    interface MapFavoriteListener {
        fun onMapFavorite(map: Map, formerPosition: Int)
    }

    /**
     * The view for each [Map]
     */
    class MapViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardView: CardView = itemView.findViewById(R.id.cv)
        var mapName: TextView = itemView.findViewById(R.id.map_name)
        var calibrationStatus: TextView = itemView.findViewById(R.id.map_calibration_status)
        var mapImage: ImageView = itemView.findViewById(R.id.map_preview_image)
        var editButton: Button = itemView.findViewById(R.id.map_manage_btn)
        var deleteButton: ImageButton = itemView.findViewById(R.id.map_delete_btn)
        var favoriteButton: ImageButton = itemView.findViewById(R.id.map_favorite_btn)
    }
}