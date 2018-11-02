package com.peterlaurence.trekadvisor.menu.mapcreate

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.MapSource
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekadvisor.menu.mapcreate.MapSourceAdapter.MapSourceSelectionListener

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @author peterLaurence on 08/04/18
 */
class MapCreateFragment : Fragment(), MapSourceSelectionListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var mapSourceSet: Array<MapSource>
    private lateinit var nextButton: Button
    private lateinit var settingsButton: Button

    private lateinit var selectedMapSource: MapSource
    private lateinit var fragmentListener: MapCreateFragmentInteractionListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MapCreateFragmentInteractionListener) {
            fragmentListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement MapCreateFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapSourceSet = MapSourceCredentials.supportedMapSource
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map_create, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = view.findViewById(R.id.next_button)
        nextButton.setOnClickListener { fragmentListener.onMapSourceSelected(selectedMapSource) }

        settingsButton = view.findViewById(R.id.mapcreate_settings_button)
        settingsButton.setOnClickListener { fragmentListener.onMapSourceSettings(selectedMapSource) }

        val viewManager = LinearLayoutManager(context)
        viewAdapter = MapSourceAdapter(mapSourceSet, this, context?.getColor(R.color.colorAccent)
                ?: Color.BLUE,
                context?.getColor(R.color.colorPrimaryTextWhite)
                        ?: Color.WHITE, context?.getColor(R.color.colorPrimaryTextBlack)
                ?: Color.BLACK)

        /* Item decoration : divider */
        val dividerItemDecoration = DividerItemDecoration(context,
                DividerItemDecoration.VERTICAL)
        val divider = this.context?.getDrawable(R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }

        recyclerView = view.findViewById<RecyclerView>(R.id.recyclerview_map_create).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(dividerItemDecoration)
        }
    }

    /**
     * For instance, settings are only relevant for [MapSource.IGN] provider.
     */
    private fun setButtonsAvailability(m: MapSource) {
        nextButton.visibility = View.VISIBLE

        settingsButton.visibility = if (m == MapSource.IGN) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun onMapSourceSelected(m: MapSource) {
        selectedMapSource = m
        setButtonsAvailability(m)
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    interface MapCreateFragmentInteractionListener {
        fun onMapSourceSelected(mapSource: MapSource)
        fun onMapSourceSettings(mapSource: MapSource)
    }
}