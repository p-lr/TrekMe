package com.peterlaurence.trekme.ui.maplist

import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader.MapDeletedListener
import com.peterlaurence.trekme.model.map.MapModel.setSettingsMap
import com.peterlaurence.trekme.ui.maplist.MapAdapter.*
import com.peterlaurence.trekme.ui.maplist.MapSettingsFragment.ConfirmDeleteFragment
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel
import java.lang.ref.WeakReference

/**
 * A [Fragment] that displays the list of available maps, using a [RecyclerView].
 *
 * Activities that contain this fragment must implement the
 * [MapListFragment.OnMapListFragmentInteractionListener] interface to handle interaction
 * events.
 */
class MapListFragment : Fragment(), MapSelectionListener, MapSettingsListener, MapDeleteListener, MapDeletedListener {
    private var rootView: FrameLayout? = null
    private var llm: LinearLayoutManager? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: MapAdapter? = null
    private var viewModel: MapListViewModel? = null
    private var listener: OnMapListFragmentInteractionListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OnMapListFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnMapListFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val viewModel = ViewModelProvider(requireActivity()).get(MapListViewModel::class.java)
        this.viewModel = viewModel
        viewModel.maps.observe(this, Observer { maps: List<Map>? ->
            if (maps != null) {
                /* Set data */
                onMapListUpdate(maps)

                /* Restore the recyclerView state if the device was rotated */
                val llmState: Parcelable?
                if (savedInstanceState != null) {
                    llmState = savedInstanceState.getParcelable(llmStateKey)
                    llm?.onRestoreInstanceState(llmState)
                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_map_list, container, false) as FrameLayout
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()

        /* When modifications happened outside of the context of this fragment, e.g if a map image
         * was changed in the settings fragment, we need to refresh the view. */
        adapter?.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        if (recyclerView != null) {
            return
        }
        generateMapList()
    }

    private fun generateMapList() {
        val ctx = context
        if (ctx != null) {
            val recyclerView = RecyclerView(ctx)
            this.recyclerView = recyclerView
            recyclerView.setHasFixedSize(false)
            llm = LinearLayoutManager(ctx)
            recyclerView.layoutManager = llm
            adapter = MapAdapter(null, this, this, this,
                    ctx.getColor(R.color.colorAccent),
                    ctx.getColor(R.color.colorPrimaryTextWhite),
                    ctx.getColor(R.color.colorPrimaryTextBlack),
                    resources)
            recyclerView.adapter = adapter
            rootView?.addView(recyclerView, 0)
        }
    }

    override fun onMapSelected(map: Map) {
        viewModel?.setMap(map)
        listener?.onMapSelectedFragmentInteraction(map)
    }

    /**
     * This fragment and its [MapAdapter] need to take action on map list update.
     */
    private fun onMapListUpdate(mapList: List<Map>) {
        val rootView = rootView ?: return
        val adapter = adapter ?: return
        rootView.findViewById<View>(R.id.loadingPanel).visibility = View.GONE
        adapter.onMapListUpdate(mapList)

        /* If no maps found, suggest to navigate to map creation */
        if (mapList.isEmpty()) {
            rootView.findViewById<View>(R.id.emptyMapPanel).visibility = View.VISIBLE
            val btn = rootView.findViewById<Button>(R.id.button_go_to_map_create)
            btn.setOnClickListener { listener?.onGoToMapCreation() }

            /* Specifically for Android 10, temporarily explain why the list of map is empty. */
            if (Build.VERSION.SDK_INT == VERSION_CODES.Q) {
                rootView.findViewById<View>(R.id.android10_warning).visibility = View.VISIBLE
            }
        } else {
            rootView.findViewById<View>(R.id.emptyMapPanel).visibility = View.GONE
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onMapSettings(map: Map) {
        setSettingsMap(map)
        if (listener != null) {
            listener!!.onMapSettingsFragmentInteraction(map)
        }
    }

    override fun onMapDelete(map: Map) {
        val f = ConfirmDeleteFragment()
        f.setMapWeakRef(WeakReference(map))
        f.setDeleteMapListener(this)
        val fragmentManager = parentFragmentManager
        f.show(fragmentManager, "delete")
    }

    override fun onMapDeleted() {
        adapter?.notifyDataSetChanged()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val llm = llm ?: return
        val llmState = llm.onSaveInstanceState()
        outState.putParcelable(llmStateKey, llmState)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface OnMapListFragmentInteractionListener {
        fun onMapSelectedFragmentInteraction(map: Map)
        fun onMapSettingsFragmentInteraction(map: Map)
        fun onGoToMapCreation()
    }

    companion object {
        private const val llmStateKey = "llmState"
    }
}