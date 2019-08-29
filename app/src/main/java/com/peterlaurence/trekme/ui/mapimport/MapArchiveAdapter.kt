package com.peterlaurence.trekme.ui.mapimport

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.viewmodel.mapimport.MapImportViewModel

/**
 * Adapter to provide access to the data set (here a list of [MapImportViewModel.ItemViewModel]).
 * For example purpose, one of the view components that is only visible when the user extracts a map
 * is loaded using a [ViewStub]. So it is only inflated at the very last moment, not at layout
 * inflation.
 *
 * @author peterLaurence on 08/06/16 -- Converted to Kotlin on 19/01/19
 */
class MapArchiveAdapter : RecyclerView.Adapter<MapArchiveViewHolder>() {

    private var data: List<MapImportViewModel.ItemViewModel>? = null
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapArchiveViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.map_archive_card, parent, false)

        return MapArchiveViewHolder(v)
    }

    override fun onBindViewHolder(holder: MapArchiveViewHolder, position: Int) {
        val data = data ?: return
        val viewModel = data[position]
        val mapArchive = data[position].mapArchive
        holder.mapArchiveName.text = mapArchive.name

        if (selectedPosition == position) {
            holder.layout.setBackgroundColor(Color.parseColor("#442196f3"))
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(-0x121213)
            } else {
                holder.layout.setBackgroundColor(-0x1)
            }
        }

        viewModel.bind(object : MapImportViewModel.ItemPresenter {

            override fun onProgress(progress: Int) {
                holder.onProgress(progress)
            }

            override fun onUnzipFinished() {
                holder.onUnzipFinished()
            }

            override fun onUnzipError() {
                holder.onUnzipError()
            }

            override fun onMapImported(map: Map, status: MapImporter.MapParserStatus) {
                holder.onMapImported(status)
            }
        })
    }

    override fun getItemCount(): Int {
        return if (data == null) 0 else data!!.size
    }


    fun setMapArchiveList(mapArchiveList: List<MapImportViewModel.ItemViewModel>) {
        data = mapArchiveList
        notifyDataSetChanged()
    }

    fun setSelectedPosition(pos: Int) {
        selectedPosition = pos
    }
}
