package com.peterlaurence.trekme.ui.mapview.components.tracksmanage

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.databinding.FragmentTracksManageBinding
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.dialogs.ColorSelectDialog
import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
import com.peterlaurence.trekme.viewmodel.mapview.TracksManageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * A fragment that shows the routes currently available for a given map, and
 * provides the ability to:
 *
 * * Rename existing tracks,
 * * Import new tracks from existing GPX files.
 *
 * @author P.Laurence on 01/03/17 -- Converted to Kotlin on 24/04/19
 */
@AndroidEntryPoint
class TracksManageFragment : Fragment(), TrackAdapter.TrackSelectionListener {
    private var _binding: FragmentTracksManageBinding? = null
    private val binding get() = _binding!!

    private var trackRenameMenuItem: MenuItem? = null
    private var trackAdapter: TrackAdapter? = null
    private val viewModel: TracksManageViewModel by viewModels()

    @Inject
    lateinit var eventBus: MapViewEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentTracksManageBinding.inflate(inflater, container, false)

        initViews()

        viewModel.tracks.observe(viewLifecycleOwner) {
            it?.also { routes ->
                trackAdapter?.setRouteList(routes)
                updateEmptyRoutePanelVisibility()
            }
        }

        if (savedInstanceState != null) {
            val routeIndex = savedInstanceState.getInt(ROUTE_INDEX)
            if (routeIndex >= 0) {
                trackAdapter?.restoreSelectionIndex(routeIndex)
            }
        }

        binding.floatingActionButton.setOnClickListener {
            onCreateTrack()
        }

        lifecycleScope.launch {
            eventBus.trackNameChangeSignal.collect {
                trackAdapter?.notifyDataSetChanged()
            }
        }

        lifecycleScope.launch {
            eventBus.trackColorChangeEvent.collect {
                viewModel.changeRouteColor(it.routeId, it.color)
                trackAdapter?.notifyDataSetChanged()
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        trackAdapter = null
        trackRenameMenuItem = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_fragment_tracks_manage, menu)
        trackRenameMenuItem = menu.findItem(R.id.track_rename_id)
        trackRenameMenuItem?.isVisible = trackAdapter?.selectedRouteIndex ?: -1 >= 0
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.track_rename_id -> {
                val map = viewModel.map ?: return true
                val selectedRoute = trackAdapter?.selectedRoute ?: return true
                val fragment = ChangeRouteNameFragment.newInstance(map.id, selectedRoute)
                fragment.show(parentFragmentManager, "rename route")
                return super.onOptionsItemSelected(item)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        /* Check if the request code is the one we are interested in */
        if (requestCode == TRACK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val ctx = context ?: return
            val uri = resultData?.data ?: return

            if (!viewModel.isFileSupported(uri)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setView(View.inflate(context, R.layout.track_warning, null))
                builder.setCancelable(false)
                        .setPositiveButton(getString(R.string.ok_dialog), null)
                val alert = builder.create()
                alert.show()
            }

            /* Import the file */
            // TODO: this shouldn't be done inside the lifecycleScope of this fragment
            lifecycleScope.launch {
                try {
                    val result = viewModel.applyGpxUri(uri)
                    if (result is TrackImporter.GpxImportResult.GpxImportOk) {
                        onGpxParseResult(result)
                    }
                } catch (e: FileNotFoundException) {
                    onError(e.message ?: "")
                } catch (e: TrackImporter.GpxParseException) {
                    onError("Error with GPX file with uri $uri")
                }
            }
        }
    }

    private fun onGpxParseResult(event: TrackImporter.GpxImportResult.GpxImportOk) {
        /* Display to the user a recap of how many tracks and waypoints were imported */
        val activity = activity
        if (activity != null) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(getString(R.string.import_result_title))
            val view = View.inflate(context, R.layout.import_gpx_result, null)

            val trackCountTextView = view.findViewById<TextView>(R.id.tracksCount)
            trackCountTextView.text = event.newRouteCount.toString()
            val waypointCountTextView = view.findViewById<TextView>(R.id.waypointsCount)
            waypointCountTextView.text = event.newMarkersCount.toString()

            builder.setView(view)
            builder.show()
        }

        /* Since new routes may have added, update the empty panel visibility */
        updateEmptyRoutePanelVisibility()

        /* Save */
        viewModel.saveChanges()
    }

    private fun initViews() {
        val ctx = context ?: return
        val recyclerView = binding.recyclerView
        recyclerView.setHasFixedSize(false)

        /* All cards are laid out vertically */
        val llm = LinearLayoutManager(ctx)
        recyclerView.layoutManager = llm

        /* Apply item decoration (add an horizontal divider) */
        val dividerItemDecoration = DividerItemDecoration(ctx,
                DividerItemDecoration.VERTICAL)
        val divider = getDrawable(ctx, R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }
        recyclerView.addItemDecoration(dividerItemDecoration)

        trackAdapter = TrackAdapter(this, ctx.getColor(R.color.colorAccent),
                ctx.getColor(R.color.colorPrimaryTextWhite),
                ctx.getColor(R.color.colorPrimaryTextBlack))
        recyclerView.adapter = trackAdapter

        /* Swipe to dismiss functionality */
        val simpleCallback = object : ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                /* Remove the track from the list and from the map */
                trackAdapter?.getRouteAt(viewHolder.adapterPosition)?.also {
                    viewModel.removeRoute(it)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun onError(message: String) {
        val view = view ?: return
        Snackbar.make(view, R.string.gpx_import_error_msg, Snackbar.LENGTH_LONG).show()
        Log.e(TAG, message)
    }

    override fun onTrackSelected() {
        trackRenameMenuItem?.isVisible = true
    }

    override fun onColorButtonClicked(route: RouteGson.Route) {
        ColorSelectDialog.newInstance(route.id, route.color).show(requireActivity().supportFragmentManager, "ColorSelectDialog")
    }

    override fun onVisibilityToggle(route: RouteGson.Route) {
        viewModel.saveChanges()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ROUTE_INDEX, trackAdapter?.selectedRouteIndex ?: -1)
    }

    /* Show or hide the panel indicating that there is no routes */
    private fun updateEmptyRoutePanelVisibility() {
        val itemCount = viewModel.tracks.value?.size ?: 0
        if (itemCount > 0) {
            binding.emptyRoutePanel.visibility = View.GONE
        } else {
            binding.emptyRoutePanel.visibility = View.VISIBLE
        }
    }

    private fun onCreateTrack() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        /* Search for all documents available via installed storage providers */
        intent.type = "*/*"
        startActivityForResult(intent, TRACK_REQUEST_CODE)
    }

    interface TrackChangeListener {
        /**
         * When new [RouteGson.Route] are added or modified, this method is called.
         *
         * @param map       the [Map] associated with the change
         * @param routeList a list of [RouteGson.Route]
         */
        fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>)

        /**
         * When the visibility of a [RouteGson.Route] is changed, this method is called.
         */
        fun onTrackVisibilityChanged()
    }

    @AndroidEntryPoint
    class ChangeRouteNameFragment : DialogFragment() {
        private val viewModel: TracksManageViewModel by viewModels()

        companion object {
            const val ROUTE_KEY = "route"
            const val MAP_ID = "mapId"

            fun newInstance(mapId: Int, route: RouteGson.Route): ChangeRouteNameFragment {
                val bundle = Bundle()
                bundle.putInt(MAP_ID, mapId)
                bundle.putSerializable(ROUTE_KEY, route)
                val fragment = ChangeRouteNameFragment()
                fragment.arguments = bundle
                return fragment
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(requireContext())
            val view = View.inflate(context, R.layout.change_trackname_fragment, null)
            val editText = view.findViewById<View>(R.id.track_name_edittext) as EditText

            val route = arguments?.get(ROUTE_KEY) as? RouteGson.Route
            val mapId = arguments?.get(MAP_ID) as? Int

            if (route != null) {
                editText.setText(route.name)
            }

            builder.setView(view)
            builder.setMessage(R.string.track_name_change)
                    .setPositiveButton(R.string.ok_dialog) { _, _ ->
                        if (mapId != null && route != null) {
                            val newName = editText.text.toString()
                            viewModel.renameRoute(route, newName)
                        }
                    }
                    .setNegativeButton(R.string.cancel_dialog_string) { _, _ ->
                        // Do nothing. This empty listener is used just to create the Cancel button.
                    }
            return builder.create()
        }
    }

    companion object {
        const val TAG = "TracksManageFragment"
        private const val TRACK_REQUEST_CODE = 1337
        private const val ROUTE_INDEX = "routeIndex"
    }
}
