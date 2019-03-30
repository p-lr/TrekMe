package com.peterlaurence.trekme.ui.mapview.components.tracksmanage

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.core.track.TrackImporter.applyGpxUriToMapAsync
import com.peterlaurence.trekme.model.map.MapProvider
import com.peterlaurence.trekme.ui.mapview.events.TrackVisibilityChangedEvent
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.FileNotFoundException
import kotlin.coroutines.CoroutineContext

/**
 * A [Fragment] subclass that shows the routes currently available for a given map, and
 * provides the ability to import new routes.
 *
 * @author peterLaurence on 01/03/17 -- Converted to Kotlin on 24/04/19
 */
class TracksManageFragment : Fragment(),
        TrackAdapter.TrackSelectionListener,
        CoroutineScope {
    private lateinit var rootView: FrameLayout
    private lateinit var emptyRoutePanel: ConstraintLayout
    private var map: Map? = null
    private var trackRenameMenuItem: MenuItem? = null
    private var trackAdapter: TrackAdapter? = null
    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        rootView = inflater.inflate(R.layout.fragment_tracks_manage, container, false) as FrameLayout
        emptyRoutePanel = rootView.findViewById(R.id.emptyRoutePanel)
        map = MapProvider.getCurrentMap()
        map?.let {
            generateTracks(it)
        }


        if (savedInstanceState != null) {
            val routeIndex = savedInstanceState.getInt(ROUTE_INDEX)
            if (routeIndex >= 0) {
                trackAdapter?.restoreSelectionIndex(routeIndex)
            }
        }

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_fragment_tracks_manage, menu)
        trackRenameMenuItem = menu.findItem(R.id.track_rename_id)
        trackRenameMenuItem?.isVisible = trackAdapter?.selectedRouteIndex ?: -1 >= 0
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
        job = Job()

        updateEmptyRoutePanelVisibility()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.import_tracks_id -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)

                /* Search for all documents available via installed storage providers */
                intent.type = "*/*"
                startActivityForResult(intent, TRACK_REQUEST_CODE)
                return true
            }
            R.id.track_rename_id -> {
                val fragment = ChangeRouteNameFragment()
                fragmentManager?.let {
                    fragment.show(it, "rename route")
                }
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

            if (!TrackImporter.isFileSupported(uri)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setView(View.inflate(context, R.layout.track_warning, null))
                builder.setCancelable(false)
                        .setPositiveButton(getString(R.string.ok_dialog), null)
                val alert = builder.create()
                alert.show()
            }

            /* Import the file */
            map?.let {
                launch {
                    try {
                        applyGpxUri(uri, it, ctx)
                    } catch (e: FileNotFoundException) {
                        onError(e.message ?: "")
                    } catch (e: TrackImporter.GpxParseException) {
                        onError("Error with GPX file with uri $uri")
                    }
                }
            }
        }
    }

    private suspend fun applyGpxUri(uri: Uri, map: Map, ctx: Context) = coroutineScope {
        applyGpxUriToMapAsync(uri, ctx.contentResolver, map).await()
    }

    @Subscribe
    fun onTrackChangedEvent(event: TrackImporter.GpxParseResult) {
        /* We want to append new routes, so the index to add new routes is equal to current length
         * of the data set. */
        val trackAdapter = trackAdapter ?: return
        val positionStart = trackAdapter.itemCount
        trackAdapter.notifyItemRangeInserted(positionStart, event.newRouteCount)

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
        saveChanges()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    /**
     * The [job] is cancelled in [onDestroy] instead of in [onStop] because in this fragment the
     * [applyGpxUri] coroutine is started **after** [onStop] and before [onStart].
     */
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun generateTracks(map: Map) {
        val ctx = context ?: return
        val recyclerView = RecyclerView(ctx)
        recyclerView.setHasFixedSize(false)

        /* All cards are laid out vertically */
        val llm = LinearLayoutManager(ctx)
        recyclerView.layoutManager = llm

        /* Apply item decoration (add an horizontal divider) */
        val dividerItemDecoration = DividerItemDecoration(ctx,
                DividerItemDecoration.VERTICAL)
        val divider = ctx.getDrawable(R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }
        recyclerView.addItemDecoration(dividerItemDecoration)

        trackAdapter = TrackAdapter(map, this, ctx.getColor(R.color.colorAccent),
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
                trackAdapter!!.removeItem(viewHolder.adapterPosition)

                /* Update the view */
                EventBus.getDefault().post(TrackVisibilityChangedEvent())

                /* Save */
                saveChanges()
            }
        }

        val itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        rootView.addView(recyclerView, 0)
    }

    private fun saveChanges() {
        MapLoader.saveRoutes(map!!)
    }

    private fun onError(message: String) {
        val view = view ?: return
        val snackbar = Snackbar.make(view, R.string.gpx_import_error_msg, Snackbar.LENGTH_LONG)
        snackbar.show()
        Log.e(TAG, message)
    }

    override fun onTrackSelected() {
        trackRenameMenuItem!!.isVisible = true
    }

    override fun onVisibilityToggle(route: RouteGson.Route) {
        EventBus.getDefault().post(TrackVisibilityChangedEvent())

        /* Save */
        saveChanges()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ROUTE_INDEX, trackAdapter!!.selectedRouteIndex)
    }

    /* Show or hide the panel indicating that there is no routes */
    private fun updateEmptyRoutePanelVisibility() {
        val itemCount = trackAdapter?.itemCount ?: 0
        if (itemCount > 0) {
            emptyRoutePanel.visibility = View.GONE
        } else {
            emptyRoutePanel.visibility = View.VISIBLE
        }
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

    class ChangeRouteNameFragment : DialogFragment() {
        private lateinit var tracksManageFragment: TracksManageFragment
        private var text: String? = null
        private lateinit var mainActivity: MainActivity

        /**
         * The first time this fragment is created, the activity exists and so does the
         * [TracksManageFragment]. But, upon configuration change, the
         * [TracksManageFragment] is not yet attached to the fragment manager when
         * [.onAttach] is called. <br></br>
         * So, we get a reference to it later in [.onActivityCreated].
         * In the meanwhile, we don't have to retrieve the route's name because the framework
         * automatically saves the [EditText] state upon configuration change.
         */
        override fun onAttach(context: Context) {
            super.onAttach(context)

            try {
                mainActivity = activity as MainActivity? ?: return
                tracksManageFragment = mainActivity.tracksManageFragment

                val route = tracksManageFragment.trackAdapter?.selectedRoute ?: return
                text = route.name
            } catch (e: NullPointerException) {
                /* The fragment is being recreated upon configuration change */
            }

        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)

            tracksManageFragment = mainActivity.tracksManageFragment
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(mainActivity)
            val view = View.inflate(context, R.layout.change_trackname_fragment, null)

            if (text != null) {
                val editText = view.findViewById<View>(R.id.track_name_edittext) as EditText
                editText.setText(text)
            }

            builder.setView(view)
            builder.setMessage(R.string.track_name_change)
                    .setPositiveButton(R.string.ok_dialog) { _, _ ->
                        val route = tracksManageFragment.trackAdapter?.selectedRoute
                        val editText = view.findViewById<View>(R.id.track_name_edittext) as EditText
                        if (route != null) {
                            route.name = editText.text.toString()
                            tracksManageFragment.trackAdapter?.notifyDataSetChanged()
                            tracksManageFragment.saveChanges()
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
