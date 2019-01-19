package com.peterlaurence.trekme.ui.mapimport

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.MapArchiveListUpdateEvent
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.maparchiver.MapArchiver
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.events.DrawerClosedEvent
import com.peterlaurence.trekme.ui.events.MapImportedEvent
import com.peterlaurence.trekme.ui.events.RequestImportMapEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipErrorEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipFinishedEvent
import com.peterlaurence.trekme.ui.mapimport.events.UnzipProgressionEvent
import com.peterlaurence.trekme.ui.tools.RecyclerItemClickListener
import com.peterlaurence.trekme.util.UnzipTask
import kotlinx.android.synthetic.main.fragment_map_import.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

/**
 * A [Fragment] subclass that displays the list of maps archives available for import.
 *
 * @author peterLaurence on 08/06/16 -- Converted to Kotlin on 18/01/19
 */
class MapImportFragment : Fragment() {
    private var mapArchiveAdapter: MapArchiveAdapter? = null
    private var listener: OnMapArchiveFragmentInteractionListener? = null
    private var mView: View? = null
    private var createAfterScreenRotation = false
    private var fabEnabled = false
    private lateinit var mapArchiveList: List<MapArchive>
    private var mapArchiveSelected: MapArchive? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnMapArchiveFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnMapArchiveFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_map_import, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mView = view

        /* When this fragment is created from a screen rotating, don't wait the drawer layout to
         * close to re-generate the map list.
         */
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean(CREATE_FROM_SCREEN_ROTATE)) {
                createAfterScreenRotation = true
            }
        }

        recyclerViewMapImport.setHasFixedSize(false)

        val llm = LinearLayoutManager(context)
        recyclerViewMapImport.layoutManager = llm

        mapArchiveAdapter = MapArchiveAdapter()
        recyclerViewMapImport.adapter = mapArchiveAdapter

        recyclerViewMapImport.addOnItemTouchListener(RecyclerItemClickListener(this.context,
                recyclerViewMapImport, object : RecyclerItemClickListener.OnItemClickListener {

            override fun onItemClick(view: View, position: Int) {
                fab.activate()
                singleSelect(position)
            }

            override fun onItemLongClick(view: View?, position: Int) {
                // no-op
            }
        }))

        /* Item decoration : divider */
        val dividerItemDecoration = DividerItemDecoration(view.context,
                DividerItemDecoration.VERTICAL)
        val divider = view.context.getDrawable(R.drawable.divider)
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }
        recyclerViewMapImport.addItemDecoration(dividerItemDecoration)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        if (mapArchiveAdapter != null) {
            mapArchiveAdapter!!.subscribeEventBus()
        }
        /* Request archive generation after EventBus registration to be 100% sure to get notified
         * if this fragment is still registered when the job finishes.
         */
        if (createAfterScreenRotation) {
            generateMapList()
        }
    }

    private fun generateMapList() {
        MapLoader.getInstance().generateMapArchives()
    }

    private fun singleSelect(position: Int) {
        /* Update adapter to reflect selection */
        mapArchiveAdapter?.setSelectedPosition(position)
        mapArchiveAdapter?.notifyDataSetChanged()

        /* Keep a reference on the selected archive */
        mapArchiveSelected = mapArchiveList[position]
    }

    private fun FloatingActionButton.activate() {
        if (!fabEnabled) {
            fab.isEnabled = true
            fab.drawable.mutate().setTint(resources.getColor(R.color.colorWhite, null))
            fab.background.setTint(resources.getColor(R.color.colorAccent, null))

            fab.setOnClickListener {
                mapArchiveSelected?.let { mapArchive ->
                    MapArchiver.archiveMap(mapArchive, object : UnzipTask.UnzipProgressionListener {

                        override fun onProgress(p: Int) {
                            EventBus.getDefault().post(UnzipProgressionEvent(mapArchive.id, p))
                        }

                        override fun onUnzipFinished(outputDirectory: File) {
                            EventBus.getDefault().post(UnzipFinishedEvent(mapArchive.id, outputDirectory))
                        }

                        override fun onUnzipError() {
                            EventBus.getDefault().post(UnzipErrorEvent(mapArchive.id))
                        }
                    })
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMapImported(event: MapImportedEvent) {
        val snackbar = Snackbar.make(view!!, R.string.snack_msg_show_map_list, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.ok_dialog) { v -> listener!!.onMapArchiveFragmentInteraction() }
        snackbar.show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRequestImportMapEvent(event: RequestImportMapEvent) {
        val confirmImport = context!!.getString(R.string.confirm_import)
        val snackbar = Snackbar.make(view!!, confirmImport, Snackbar.LENGTH_LONG)
        snackbar.show()
    }

    /**
     * When this fragment is created for the first time, we wait the [DrawerLayout]
     * to close before generating the map list (to avoid a stutter). <br></br>
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDrawerClosed(event: DrawerClosedEvent) {
        generateMapList()
    }

    /**
     * A [MapArchiveListUpdateEvent] is emitted from the [MapLoader] when the list of
     * map archives is updated.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMapArchiveListUpdate(event: MapArchiveListUpdateEvent) {
        mapArchiveList = MapLoader.getInstance().mapArchives
        hideProgressBar()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        if (mapArchiveAdapter != null) {
            mapArchiveAdapter!!.unSubscribeEventBus()
        }
        super.onStop()

        if (recyclerViewMapImport == null) return
        for (i in 0 until recyclerViewMapImport.adapter!!.itemCount) {
            val holder = recyclerViewMapImport.findViewHolderForAdapterPosition(i) as MapArchiveViewHolder?
            holder?.unSubscribe()
        }
    }

    private fun hideProgressBar() {
        view!!.findViewById<View>(R.id.progress_import_frgmt).visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean(CREATE_FROM_SCREEN_ROTATE, true)
    }

    interface OnMapArchiveFragmentInteractionListener {
        fun onMapArchiveFragmentInteraction()
    }

    companion object {
        private const val CREATE_FROM_SCREEN_ROTATE = "create"
    }
}
