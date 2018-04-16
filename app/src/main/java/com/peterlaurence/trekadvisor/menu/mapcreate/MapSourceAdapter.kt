package com.peterlaurence.trekadvisor.menu.mapcreate

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.MapSource

class MapSourceAdapter(private val mapSourceSet: Array<MapSource>) :
        RecyclerView.Adapter<MapSourceAdapter.MapSourceViewHolder>() {
    lateinit var parentView: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapSourceViewHolder {
        parentView = parent
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.map_source_card, parent, false) as CardView
        return MapSourceViewHolder(cardView)
    }

    override fun getItemCount(): Int {
        return mapSourceSet.size
    }

    override fun onBindViewHolder(holder: MapSourceViewHolder?, position: Int) {
        val mapSource = mapSourceSet[position]
        when (mapSource) {
            MapSource.IGN -> {
                holder?.title?.text = parentView.resources.getText(R.string.ign_source)
                holder?.description?.text = parentView.resources.getText(R.string.ign_source_description)
                holder?.image?.setImageDrawable(parentView.resources.getDrawable(R.drawable.ign_logo, null))
            }
            MapSource.OPEN_STREET_MAP -> {
                holder?.title?.text = parentView.resources.getText(R.string.open_street_map_source)
                holder?.description?.text = parentView.resources.getText(
                        R.string.open_street_map_source_description)
            }
        }
    }

    class MapSourceViewHolder(cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        var title: TextView = cardView.findViewById(R.id.map_source_title)
        var description: TextView = cardView.findViewById(R.id.map_source_description)
        var image: ImageView = cardView.findViewById(R.id.map_source_image)
    }
}