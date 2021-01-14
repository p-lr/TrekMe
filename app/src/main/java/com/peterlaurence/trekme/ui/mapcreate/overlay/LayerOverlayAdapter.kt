package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.databinding.LayerItemBinding

class LayerOverlayAdapter : RecyclerView.Adapter<LayerOverlayAdapter.LayerOverlayViewHolder>() {
    private val diffCallback: DiffUtil.ItemCallback<LayerInfo> = object : DiffUtil.ItemCallback<LayerInfo>() {
        override fun areItemsTheSame(oldItem: LayerInfo, newItem: LayerInfo): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }

        override fun areContentsTheSame(oldItem: LayerInfo, newItem: LayerInfo): Boolean {
            return oldItem == newItem
        }
    }
    private val differ = AsyncListDiffer(this, diffCallback)

    fun setLayerInfo(data: List<LayerInfo>) {
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
        val data = differ.currentList[position] ?: return
        holder.setTitle(data.name)

        holder.slider.value = data.properties.opacity
        holder.slider.clearOnChangeListeners()
        holder.slider.addOnChangeListener { _, value, _ ->
            data.properties.opacity = value
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    class LayerOverlayViewHolder(private val itemBinding: LayerItemBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        val view = itemBinding.root
        val slider = itemBinding.slider

        fun setTitle(title: String) {
            itemBinding.title.text = title
        }
    }
}