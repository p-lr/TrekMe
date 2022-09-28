package com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.ParcelUuid
import android.view.*
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getDrawable
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.databinding.FragmentTracksManageBinding
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.dialogs.ColorSelectDialog
import com.peterlaurence.trekme.features.map.presentation.ui.legacy.events.TracksEventBus
import com.peterlaurence.trekme.util.collectWhileResumedIn
import com.peterlaurence.trekme.features.map.presentation.viewmodel.TracksManageViewModel
import com.peterlaurence.trekme.util.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import java.util.*
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
    private var binding: FragmentTracksManageBinding? = null

    private var trackRenameMenuItem: MenuItem? = null
    private var trackGoToMapMenuItem: MenuItem? = null
    private var trackAdapter: TrackAdapter? = null
    private val viewModel: TracksManageViewModel by viewModels()

    @Inject
    lateinit var eventBus: TracksEventBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Going back to the map and center on a route is only available in the extended offer. */
        viewModel.hasExtendedOffer.observe(this) {
            it?.also { visible ->
                if (visible) {
                    trackGoToMapMenuItem?.isVisible = it
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }

        eventBus.trackNameChangeSignal.map {
            trackAdapter?.notifyDataSetChanged()
        }.collectWhileResumedIn(this)

        eventBus.trackColorChangeEvent.map {
            viewModel.changeRouteColor(it.routeId, it.color)
            trackAdapter?.notifyDataSetChanged()
        }.collectWhileResumedIn(this)

        eventBus.trackImportEvent.map {
            if (it is GeoRecordImportResult.GeoRecordImportOk) {
                onGpxParseResult(it)
            } else {
                onImportError()
            }
        }.collectWhileResumedIn(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        /* The action bar is managed by the view-system */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.tracks_manage_frgmt_title)
            show()
        }

        val binding = FragmentTracksManageBinding.inflate(inflater, container, false)
        this.binding = binding

        initViews()

        viewModel.tracks.observe(viewLifecycleOwner) {
            it?.also { routes ->
                trackAdapter?.setRouteList(routes)
                updateEmptyRoutePanelVisibility()
                updateMenuVisibility(routes.size)
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
        trackAdapter = null
        trackRenameMenuItem = null
        trackGoToMapMenuItem = null
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_fragment_tracks_manage, menu)
                trackRenameMenuItem = menu.findItem(R.id.track_rename_id)
                trackGoToMapMenuItem = menu.findItem(R.id.track_go_to_map_id)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                val selectedRoute = trackAdapter?.selectedRoute ?: return true
                return when (menuItem.itemId) {
                    R.id.track_rename_id -> {
                        val map = viewModel.map ?: return true
                        val fragment = ChangeRouteNameFragment.newInstance(map.id, selectedRoute.id)
                        fragment.show(parentFragmentManager, "rename route")
                        true
                    }
                    R.id.track_go_to_map_id -> {
                        findNavController().navigateUp()
                        viewModel.goToRouteOnMap(selectedRoute)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun onGpxParseResult(event: GeoRecordImportResult.GeoRecordImportOk) {
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
    }

    private fun initViews() {
        val ctx = context ?: return
        val recyclerView = binding?.recyclerView ?: return
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
                trackAdapter?.getRouteAt(viewHolder.bindingAdapterPosition)?.also {
                    viewModel.removeRoute(it)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun onImportError() {
        val view = view ?: return
        Snackbar.make(view, R.string.gpx_import_error_msg, Snackbar.LENGTH_LONG).show()
    }

    override fun onTrackSelected() {
        trackRenameMenuItem?.isVisible = true
        trackGoToMapMenuItem?.isVisible = viewModel.hasExtendedOffer.value ?: false
    }

    override fun onColorButtonClicked(route: Route) {
        ColorSelectDialog.newInstance(route.id, route.color.value).show(requireActivity().supportFragmentManager, "ColorSelectDialog")
    }

    override fun onVisibilityToggle(route: Route) {
        viewModel.saveChanges(route)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ROUTE_INDEX, trackAdapter?.selectedRouteIndex ?: -1)
    }

    /* Show or hide the panel indicating that there is no routes */
    private fun updateEmptyRoutePanelVisibility() {
        val binding = this.binding ?: return
        val itemCount = viewModel.tracks.value?.size ?: 0
        if (itemCount > 0) {
            binding.emptyRoutePanel.visibility = View.GONE
        } else {
            binding.emptyRoutePanel.visibility = View.VISIBLE
        }
    }

    private fun updateMenuVisibility(routeCnt: Int) {
        val selectedInRange = trackAdapter?.selectedRouteIndex ?: -1 in 0 until routeCnt
        trackRenameMenuItem?.isVisible = routeCnt > 0 && selectedInRange
    }

    private val chooseTrackLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val ctx = context ?: return@registerForActivityResult
            val uri = result?.data?.data ?: return@registerForActivityResult

            if (!viewModel.isFileSupported(uri)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setView(View.inflate(context, R.layout.track_warning, null))
                builder.setCancelable(false)
                        .setPositiveButton(getString(R.string.ok_dialog), null)
                val alert = builder.create()
                alert.show()
            }

            /* Import the file */
            viewModel.applyUri(uri)
        }
    }

    private fun onCreateTrack() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        /* Search for all documents available via installed storage providers */
        intent.type = "*/*"
        chooseTrackLauncher.launch(intent)
    }

    interface TrackChangeListener {
        /**
         * When new [Route] are added or modified, this method is called.
         *
         * @param map       the [Map] associated with the change
         * @param routeList a list of [Route]
         */
        fun onTrackChanged(map: Map, routeList: List<Route>)

        /**
         * When the visibility of a [Route] is changed, this method is called.
         */
        fun onTrackVisibilityChanged()
    }

    @AndroidEntryPoint
    class ChangeRouteNameFragment : DialogFragment() {
        private val viewModel: TracksManageViewModel by viewModels()

        companion object {
            const val ROUTE_ID = "routeId"
            const val MAP_ID = "mapId"

            fun newInstance(mapId: UUID, routeId: String): ChangeRouteNameFragment {
                val bundle = Bundle()
                bundle.putParcelable(MAP_ID, ParcelUuid(mapId))
                bundle.putString(ROUTE_ID, routeId)
                val fragment = ChangeRouteNameFragment()
                fragment.arguments = bundle
                return fragment
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(requireContext())
            val view = View.inflate(context, R.layout.change_trackname_fragment, null)
            val editText = view.findViewById<View>(R.id.track_name_edittext) as EditText

            val routeId = arguments?.getString(ROUTE_ID) ?: return builder.create()
            val mapId = arguments?.parcelable<ParcelUuid>(MAP_ID)?.uuid

            val route = viewModel.getRoute(routeId) ?: return builder.create()
            editText.setText(route.name)

            builder.setView(view)
            builder.setMessage(R.string.track_name_change)
                    .setPositiveButton(R.string.ok_dialog) { _, _ ->
                        if (mapId != null) {
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
        private const val ROUTE_INDEX = "routeIndex"
    }
}
