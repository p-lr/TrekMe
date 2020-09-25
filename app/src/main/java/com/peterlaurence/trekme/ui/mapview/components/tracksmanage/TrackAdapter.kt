package com.peterlaurence.trekme.ui.mapview.components.tracksmanage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.gson.RouteGson.Route
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TrackAdapter.TrackViewHolder

/**
 * Adapter to provide access to the data set (here a list of [RouteGson.Route]).
 *
 * @author P.Laurence on 01/03/17 -- Converted to Kotlin on 25/09/20
 */
class TrackAdapter(
        private val trackSelectionListener: TrackSelectionListener,
        private val colorAccent: Int,
        private val colorWhite: Int,
        private val colorBlack: Int
) : RecyclerView.Adapter<TrackViewHolder>() {

    var selectedRouteIndex = -1
        private set
    private var previousSelectedRouteIndex = -1
    private val diffCallback: DiffUtil.ItemCallback<Route> = object : DiffUtil.ItemCallback<Route>() {
        override fun areItemsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Route, newItem: Route): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun setRouteList(routeList: List<Route>?) {
        differ.submitList(routeList)
    }

    fun getRouteAt(position: Int): Route? {
        return try {
            differ.currentList[position]
        } catch (e: IndexOutOfBoundsException) {
            null
        }
    }

    val selectedRoute: Route?
        get() = try {
            differ.currentList[selectedRouteIndex]
        } catch (e: ArrayIndexOutOfBoundsException) {
            null
        }

    fun restoreSelectionIndex(selectionIndex: Int) {
        selectedRouteIndex = selectionIndex
        updateSelectionColor(selectionIndex)
    }

    /**
     * Simple implementation of a toggle selection. When an item is clicked, we change its
     * background and we remember his index. When another item is clicked, the background of the
     * first item is set to its original state.
     *
     * @param position index of the selected view
     */
    private fun updateSelectionColor(position: Int) {
        selectedRouteIndex = position
        notifyItemChanged(position)
        if (previousSelectedRouteIndex != -1) {
            notifyItemChanged(previousSelectedRouteIndex)
        }
        previousSelectedRouteIndex = position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.track_card, parent, false)
        return TrackViewHolder(v)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val route = differ.currentList[position] ?: return
        holder.trackName.text = route.name
        holder.setVisibleButtonIcon(route.visible)
        holder.visibleButton.setOnClickListener {
            route.toggleVisibility()
            holder.setVisibleButtonIcon(route.visible)
            trackSelectionListener.onVisibilityToggle(route)
        }
        if (holder.layoutPosition == selectedRouteIndex) {
            holder.cardView.setCardBackgroundColor(colorAccent)
            holder.trackName.setTextColor(colorWhite)
            holder.visibleButton.setColorFilter(colorWhite)
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.trackName.setTextColor(colorBlack)
            holder.visibleButton.setColorFilter(colorBlack)
        }
        holder.itemView.setOnClickListener {
            /* Toggle background color */
            updateSelectionColor(position)

            /* Call the listener for track selection */
            trackSelectionListener.onTrackSelected()
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    interface TrackSelectionListener {
        fun onTrackSelected()
        fun onVisibilityToggle(route: Route)
    }

    class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cardView: CardView = itemView.findViewById(R.id.cv_track)
        var trackName: TextView = itemView.findViewById(R.id.track_name)
        var visibleButton: ImageButton = itemView.findViewById(R.id.track_visible_btn)

        fun setVisibleButtonIcon(visible: Boolean) {
            visibleButton.setImageResource(if (visible) R.drawable.ic_visibility_black_24dp else R.drawable.ic_visibility_off_black_24dp)
        }
    }
}