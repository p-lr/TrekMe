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
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.Map.CalibrationStatus
import com.peterlaurence.trekme.model.map.MapModel
import com.peterlaurence.trekme.ui.maplist.MapAdapter.MapViewHolder

/**
 * Adapter to provide access to the data set (here a list of [Map]).
 *
 * @author peterLaurence on 26/12/15 - Converted to Kotlin on 02/07/2020
 */
class MapAdapter internal constructor(
        private val mapSelectionListener: MapSelectionListener,
        private val mapSettingsListener: MapSettingsListener,
        private val mapDeleteListener: MapDeleteListener,
        private val mapFavoriteListener: MapFavoriteListener,
        private val colorAccent: Int, private val colorWhiteText: Int,
        private val colorBlackText: Int, private val resources: Resources
) : RecyclerView.Adapter<MapViewHolder>() {
    private val diffCallback = object : DiffUtil.ItemCallback<Map>() {
        override fun areItemsTheSame(oldItem: Map, newItem: Map): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Map, newItem: Map): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun setMapList(mapList: List<Map>?) {
        differ.submitList(mapList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.map_row, parent, false)
        return MapViewHolder(v)
    }

    override fun onBindViewHolder(holder: MapViewHolder, position: Int) {
        val map = differ.currentList[position] ?: return
        val dr = RoundedBitmapDrawableFactory.create(resources, map.image)
        dr.cornerRadius = 16f

        with(holder) {
            mapImage.setImageDrawable(dr)
            mapName.text = map.name
            calibrationStatus.text = map.description
            if (map.isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_baseline_star_24)
            } else {
                favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24)
            }
            if (map == MapModel.getCurrentMap()) {
                cardView.setCardBackgroundColor(colorAccent)
                mapName.setTextColor(colorWhiteText)
                editButton.setTextColor(colorWhiteText)
                deleteButton.setColorFilter(colorWhiteText)
                calibrationStatus.setTextColor(colorWhiteText)
                favoriteButton.setColorFilter(colorWhiteText)
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
                mapName.setTextColor(colorBlackText)
                editButton.setTextColor(colorAccent)
                deleteButton.setColorFilter(colorAccent)
            }
            when (map.calibrationStatus) {
                CalibrationStatus.OK -> calibrationStatus.setText(R.string.calibration_status_ok)
                CalibrationStatus.NONE, null -> calibrationStatus.setText(R.string.calibration_status_none)
                CalibrationStatus.ERROR -> calibrationStatus.setText(R.string.calibration_status_error)
            }

            /* Set click listeners */
            itemView.setOnClickListener {
                // Call the listener for Map selection
                mapSelectionListener.onMapSelected(map)
            }
            editButton.setOnClickListener { mapSettingsListener.onMapSettings(map) }
            deleteButton.setOnClickListener { mapDeleteListener.onMapDelete(map) }
            favoriteButton.setOnClickListener {
                /* Toggle icon */
                if (map.isFavorite) {
                    favoriteButton.setImageResource(R.drawable.ic_baseline_star_border_24)
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_baseline_star_24)
                }
                mapFavoriteListener.onMapFavorite(map, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
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