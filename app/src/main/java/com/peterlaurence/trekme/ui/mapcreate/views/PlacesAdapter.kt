package com.peterlaurence.trekme.ui.mapcreate.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.databinding.GeoplaceItemBinding

/**
 * Defines how given list of [GeoPlace] are rendered.
 *
 * @author P.Laurence on 04/01/2021
 */
class PlacesAdapter : RecyclerView.Adapter<PlacesAdapter.GeoPlaceViewHolder>() {
    private val diffCallback: DiffUtil.ItemCallback<GeoPlace> = object : DiffUtil.ItemCallback<GeoPlace>() {
        override fun areItemsTheSame(oldItem: GeoPlace, newItem: GeoPlace): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: GeoPlace, newItem: GeoPlace): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun setGeoPlaces(geoPlaceList: List<GeoPlace>, cb: (() -> Unit)? = null) {
        /* Be careful always to provide a copy of the data set, otherwise modifications
         * on the data set would directly affect the adapter's AsyncListDiffer internals and cause
         * inconsistencies and crash. */
        differ.submitList(geoPlaceList.toList()) { cb?.invoke() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeoPlaceViewHolder {
        val v = GeoplaceItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GeoPlaceViewHolder(v)
    }

    override fun onBindViewHolder(holder: GeoPlaceViewHolder, position: Int) {
        val data = differ.currentList[position] ?: return
        holder.setTitle(data.name)
        holder.setSubtitle(data.locality)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    class GeoPlaceViewHolder(private val itemBinding: GeoplaceItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun setTitle(title: String) {
            itemBinding.title.text = title
        }

        fun setSubtitle(subTitle: String) {
            itemBinding.subtitle.text = subTitle
        }
    }
}