package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.databinding.LayerItemBinding
import com.peterlaurence.trekme.viewmodel.mapcreate.LayerProperties

class LayerOverlayAdapter : RecyclerView.Adapter<LayerOverlayAdapter.LayerOverlayViewHolder>() {
    private val diffCallback: DiffUtil.ItemCallback<LayerProperties> = object : DiffUtil.ItemCallback<LayerProperties>() {
        override fun areItemsTheSame(oldItem: LayerProperties, newItem: LayerProperties): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: LayerProperties, newItem: LayerProperties): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)
    
    fun setLayerProperties(data: List<LayerProperties>) {
        /* Be careful always to provide a copy of the data set, otherwise modifications
         * on the data set would directly affect the adapter's AsyncListDiffer internals and cause
         * inconsistencies and crash. */
        differ.submitList(data.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerOverlayViewHolder {
        val v = LayerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LayerOverlayViewHolder(v)
    }

    override fun onBindViewHolder(holder: LayerOverlayViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    class LayerOverlayViewHolder(private val itemBinding: LayerItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        val view = itemBinding.root

        fun setTitle(title: String) {
            itemBinding.title.text = title
        }
    }
}