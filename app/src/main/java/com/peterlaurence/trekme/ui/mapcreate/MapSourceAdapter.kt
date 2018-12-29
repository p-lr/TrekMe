package com.peterlaurence.trekme.ui.mapcreate

import android.graphics.Color
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.MapSource
import java.lang.ref.WeakReference

class MapSourceAdapter(private val mapSourceSet: Array<MapSource>, private val mapSourceSelectionListener: MapSourceSelectionListener,
                       private val accentColor: Int, private val whiteTextColor: Int, private val blackTextColor: Int) :
        RecyclerView.Adapter<MapSourceAdapter.MapSourceViewHolder>() {
    lateinit var parentView: ViewGroup

    private var selectedMapSourceIndex = -1
    private var previousSelectedMapSourceIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapSourceViewHolder {
        parentView = parent
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.map_source_card, parent, false) as CardView
        return MapSourceViewHolder(cardView)
    }

    override fun getItemCount(): Int {
        return mapSourceSet.size
    }

    override fun onBindViewHolder(holder: MapSourceViewHolder, position: Int) {
        val mapSource = mapSourceSet[position]
        when (mapSource) {
            MapSource.IGN -> {
                holder.title.text = parentView.resources.getText(R.string.ign_source)
                holder.description.text = parentView.resources.getText(R.string.ign_source_description)
                holder.image.setImageDrawable(parentView.resources.getDrawable(R.drawable.ign_logo, null))
            }
            MapSource.OPEN_STREET_MAP -> {
                holder.title.text = parentView.resources.getText(R.string.open_street_map_source)
                holder.description.text = parentView.resources.getText(
                        R.string.open_street_map_source_description)
                holder.image.setImageDrawable(parentView.resources.getDrawable(R.drawable.openstreetmap_logo, null))
            }
            MapSource.USGS -> {
                holder.title.text = parentView.resources.getText(R.string.usgs_map_source)
                holder.description.text = parentView.resources.getText(
                        R.string.usgs_map_source_description)
                holder.image.setImageDrawable(parentView.resources.getDrawable(R.drawable.usgs_logo, null))
            }
            MapSource.IGN_SPAIN -> {
                holder.title.text = parentView.resources.getText(R.string.ign_spain_source)
                holder.description.text = parentView.resources.getText(R.string.ign_spain_source_description)
                holder.image.setImageDrawable(parentView.resources.getDrawable(R.drawable.ign_spain_logo, null))
            }
        }

        /* Take the selection into account to set colors */
        if (holder.layoutPosition == selectedMapSourceIndex) {
            holder.cardView.setCardBackgroundColor(accentColor)
            holder.description.setTextColor(whiteTextColor)
            holder.title.setTextColor(whiteTextColor)
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.description.setTextColor(blackTextColor)
            holder.title.setTextColor(blackTextColor)
        }
    }

    inner class MapSourceViewHolder(val cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        var title: TextView = cardView.findViewById(R.id.map_source_title)
        var description: TextView = cardView.findViewById(R.id.map_source_description)
        var image: ImageView = cardView.findViewById(R.id.map_source_image)

        var clickListener = MapViewHolderClickListener(this@MapSourceViewHolder,
                this@MapSourceAdapter)

        init {
            cardView.setOnClickListener(clickListener)
        }
    }

    /**
     * Simple implementation of a toggle selection. When an item is clicked, we remember his index.
     * The model is notified that both the new and the previously clicked item have changed.
     *
     * @param position index of the selected view
     */
    private fun updateSelectionIndex(position: Int) {
        selectedMapSourceIndex = position
        notifyItemChanged(position)
        if (previousSelectedMapSourceIndex != -1) {
            notifyItemChanged(previousSelectedMapSourceIndex)
        }
        previousSelectedMapSourceIndex = position
    }

    class MapViewHolderClickListener internal constructor(mapViewHolder: MapSourceViewHolder, mapAdapter: MapSourceAdapter) : View.OnClickListener {
        private val mapViewHolderWeakReference: WeakReference<MapSourceViewHolder> = WeakReference(mapViewHolder)
        private val mapAdapterWeakReference: WeakReference<MapSourceAdapter> = WeakReference(mapAdapter)

        override fun onClick(v: View) {
            val mapAdapter = mapAdapterWeakReference.get()
            val mapViewHolder = mapViewHolderWeakReference.get()
            if (mapViewHolder != null && mapAdapter != null) {
                val position = mapViewHolder.adapterPosition

                /* Update selection */
                mapAdapter.updateSelectionIndex(position)

                /* Call the listener for MapSource selection */
                val mapSource = mapAdapter.mapSourceSet[position]
                mapAdapter.mapSourceSelectionListener.onMapSourceSelected(mapSource)
            }
        }
    }

    interface MapSourceSelectionListener {
        fun onMapSourceSelected(m: MapSource)
    }
}