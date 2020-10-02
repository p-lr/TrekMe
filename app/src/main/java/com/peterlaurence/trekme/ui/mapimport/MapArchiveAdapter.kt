package com.peterlaurence.trekme.ui.mapimport

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.databinding.MapArchiveCardBinding
import com.peterlaurence.trekme.viewmodel.mapimport.*

/**
 * Adapter to provide access to the data set (here a list of [MapImportViewModel.ItemData]).
 * For example purpose, one of the view components that is only visible when the user extracts a map
 * is loaded using a [ViewStub]. So it is only inflated at the very last moment, not at layout
 * inflation.
 *
 * Only this adapter can make the correspondence between an [MapImportViewModel.ItemData] and a
 * [MapArchiveViewHolder]. The [MapImportFragment] notifies this adapter of any [UnzipEvent] related
 * using [setUnzipEventForItem].
 *
 * @author peterLaurence on 08/06/16 -- Converted to Kotlin on 19/01/19
 */
class MapArchiveAdapter : RecyclerView.Adapter<MapArchiveViewHolder>() {

    private var data: List<MapImportViewModel.ItemData>? = null
    private val viewHolderForItem = mutableMapOf<MapImportViewModel.ItemData, MapArchiveViewHolder>()
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapArchiveViewHolder {
        val ctx = parent.context
        val binding = MapArchiveCardBinding.inflate(LayoutInflater.from(ctx), parent, false)

        return MapArchiveViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MapArchiveViewHolder, position: Int) {
        val data = data ?: return
        val itemViewModel = data[position]
        val item = data[position]
        holder.mapArchiveName.text = item.name

        if (selectedPosition == position) {
            holder.layout.setBackgroundColor(Color.parseColor("#442196f3"))
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(-0x121213)
            } else {
                holder.layout.setBackgroundColor(-0x1)
            }
        }

        holder.init()

        viewHolderForItem[itemViewModel] = holder
    }

    override fun getItemCount(): Int {
        return if (data == null) 0 else data!!.size
    }


    fun setMapArchiveList(mapArchiveList: List<MapImportViewModel.ItemData>) {
        data = mapArchiveList
        notifyDataSetChanged()
    }

    fun setSelectedPosition(pos: Int) {
        selectedPosition = pos
    }

    fun setUnzipEventForItem(itemData: MapImportViewModel.ItemData, event: UnzipEvent) {
        val viewHolder = viewHolderForItem[itemData] ?: return
        when (event) {
            is UnzipProgressEvent -> viewHolder.onProgress(event.p)
            is UnzipFinishedEvent -> viewHolder.onUnzipFinished()
            is UnzipErrorEvent -> viewHolder.onUnzipError()
            is UnzipMapImportedEvent -> viewHolder.onMapImported(event.status)
        }
    }
}
