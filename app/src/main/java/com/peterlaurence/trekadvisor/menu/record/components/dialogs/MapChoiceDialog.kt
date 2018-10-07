package com.peterlaurence.trekadvisor.menu.record.components.dialogs

import android.app.Dialog
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.map.Map
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader
import com.peterlaurence.trekadvisor.menu.record.components.events.MapSelectedForRecord
import org.greenrobot.eventbus.EventBus

private const val KEY_BUNDLE_MAP_INDEX = "mapIndex"

/**
 * A dialog that displays the list of maps. The user can only select one map, and confirm or not the
 * selection.
 *
 * @author peterLaurence on 01/09/2018
 */
class MapChoiceDialog : DialogFragment(), MapChoiceSelectionListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var mapChoiceAdapter: MapChoiceAdapter
    private var selectedIndex: Int = -1

    private var mapSelected: Map? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        selectedIndex = savedInstanceState?.getInt(KEY_BUNDLE_MAP_INDEX) ?: -1

        recyclerView = RecyclerView(activity!!.baseContext)
        val llm = LinearLayoutManager(activity)
        recyclerView.layoutManager = llm

        /* Fetch the list of maps */
        val mapList = MapLoader.getInstance().maps

        mapChoiceAdapter = MapChoiceAdapter(mapList, this, selectedIndex)
        recyclerView.adapter = mapChoiceAdapter

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle(getString(R.string.choose_a_map))
        builder.setView(recyclerView)
        builder.setPositiveButton(getString(R.string.ok_dialog)) { _, _ ->
            if (mapSelected != null) {
                EventBus.getDefault().post(MapSelectedForRecord(mapSelected?.id!!))
            }
        }
        builder.setNegativeButton(getString(R.string.cancel_dialog_string)) { _, _ ->
            dismiss()
        }

        return builder.create()
    }

    override fun onMapSelected(map: Map, position: Int) {
        selectedIndex = position
        mapSelected = map
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(KEY_BUNDLE_MAP_INDEX, selectedIndex)
    }
}


class MapChoiceAdapter(private val mapList: List<Map>, val listener: MapChoiceSelectionListener,
                       selectedIndex: Int) : RecyclerView.Adapter<MapChoiceViewHolder>(), MapChoiceItemClickListener {
    private var index = selectedIndex
    private var oldIndex = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapChoiceViewHolder {
        val ctx = parent.context
        val v = LayoutInflater.from(ctx).inflate(R.layout.map_choice_row, parent, false)
        return MapChoiceViewHolder(v)
    }

    override fun getItemCount(): Int {
        return mapList.size
    }

    override fun onBindViewHolder(holder: MapChoiceViewHolder, position: Int) {
        val map = mapList[position]
        holder.textView.text = map.name
        holder.index = position

        holder.setItemClickListener(this)

        if (position == index) {
            holder.layout.setBackgroundColor(0x882196F3.toInt())
        } else {
            if (position % 2 == 0) {
                holder.layout.setBackgroundColor(0xFFEDEDED.toInt())
            } else {
                holder.layout.setBackgroundColor(0xFFFFFFFF.toInt())
            }
        }
    }

    override fun onItemClick(position: Int) {
        oldIndex = index
        index = position
        notifyItemChanged(oldIndex)
        notifyItemChanged(index)

        listener.onMapSelected(mapList[position], position)
    }
}

class MapChoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val layout: ConstraintLayout = itemView.findViewById(R.id.map_choice_layout)
    val textView: TextView = itemView.findViewById(R.id.map_choice_textview)
    var index: Int = -1

    fun setItemClickListener(listener: MapChoiceItemClickListener) {
        itemView.setOnClickListener { _ -> listener.onItemClick(index) }
    }
}

interface MapChoiceItemClickListener {
    fun onItemClick(position: Int)
}

interface MapChoiceSelectionListener {
    fun onMapSelected(map: Map, position: Int)
}