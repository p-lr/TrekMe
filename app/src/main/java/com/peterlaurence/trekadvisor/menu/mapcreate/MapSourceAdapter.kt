package com.peterlaurence.trekadvisor.menu.mapcreate

import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.peterlaurence.trekadvisor.R

class MapSourceAdapter(private val mapSourceSet: Array<String>) :
        RecyclerView.Adapter<MapSourceAdapter.MapSourceViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapSourceViewHolder {
        val cardView = LayoutInflater.from(parent.context).inflate(R.layout.map_source_card, parent, false) as CardView
        return MapSourceViewHolder(cardView)
    }

    override fun getItemCount(): Int {
        return mapSourceSet.size
    }

    override fun onBindViewHolder(holder: MapSourceViewHolder?, position: Int) {
        holder?.title?.text = mapSourceSet[position]
    }

    class MapSourceViewHolder(cardView: CardView) : RecyclerView.ViewHolder(cardView) {
        var title: TextView = cardView.findViewById(R.id.map_source_title)
    }
}