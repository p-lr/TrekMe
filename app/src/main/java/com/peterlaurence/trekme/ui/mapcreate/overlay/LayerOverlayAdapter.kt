package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.databinding.LayerItemBinding

/**
 * Defines how [LayerInfo]s are laid out.
 *
 * @author P.Laurence on 2021-01-12
 */
class LayerOverlayAdapter(
        private val itemTouchHelper: ItemTouchHelper
) : RecyclerView.Adapter<LayerOverlayAdapter.LayerOverlayViewHolder>() {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerOverlayViewHolder {
        val v = LayerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LayerOverlayViewHolder(v).apply {
            dragButton.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }
                return@setOnTouchListener true
            }
        }
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
        val dragButton = itemBinding.dragButton

        fun setTitle(title: String) {
            itemBinding.title.text = title
        }
    }
}