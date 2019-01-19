package com.peterlaurence.trekme.ui.mapimport

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewStub
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.MapArchiveListUpdateEvent
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Adapter to provide access to the data set (here a list of [MapArchive]). <br></br>
 * For example purpose, one of the view components that is only visible when the user extracts a map
 * is loaded using a [ViewStub]. So it is only inflated at the very last moment, not at layout
 * inflation.
 *
 * @author peterLaurence on 08/06/16 -- Converted to Kotlin on 19/01/19
 */
class MapArchiveAdapter : RecyclerView.Adapter<MapArchiveViewHolder>() {

    private var mMapArchiveList: List<MapArchive>? = null
    private var selectedPosition = -1

    internal fun subscribeEventBus() {
        EventBus.getDefault().register(this)
    }

    internal fun unSubscribeEventBus() {
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapArchiveViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.map_archive_card, parent, false)

        return MapArchiveViewHolder(v)
    }

    override fun onBindViewHolder(holder: MapArchiveViewHolder, position: Int) {
        val mapArchive = mMapArchiveList!![position]
        holder.mArchiveId = mapArchive.id
        holder.mapArchiveName.text = mapArchive.name

        holder.subscribe()

        if (selectedPosition == position) {
            holder.layout.setBackgroundColor(-0x77de690d)
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(-0x121213)
            } else {
                holder.layout.setBackgroundColor(-0x1)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (mMapArchiveList == null) 0 else mMapArchiveList!!.size
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMapArchiveListUpdate(event: MapArchiveListUpdateEvent) {
        mMapArchiveList = MapLoader.getInstance().mapArchives
        if (mMapArchiveList != null) {
            notifyDataSetChanged()
        }
    }

    fun setSelectedPosition(pos: Int) {
        selectedPosition = pos
    }
}
