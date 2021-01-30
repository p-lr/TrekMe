package com.peterlaurence.trekme.ui.maplist

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.databinding.FragmentMapListBinding
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.ui.maplist.MapAdapter.*
import com.peterlaurence.trekme.ui.maplist.dialogs.ConfirmDeleteDialog
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A [Fragment] that displays the list of available maps, using a [RecyclerView].
 *
 * @author P.Laurence on 24/05/2020
 */
@AndroidEntryPoint
class MapListFragment : Fragment(), MapSelectionListener, MapSettingsListener, MapDeleteListener,
        MapFavoriteListener {
    private var _binding: FragmentMapListBinding? = null
    private val binding get() = _binding!!
    private val args: MapListFragmentArgs by navArgs()

    @Inject
    lateinit var mapRepository: MapRepository

    private var llm: LinearLayoutManager? = null
    private var llmState: Parcelable? = null
    private var adapter: MapAdapter? = null
    private val viewModel: MapListViewModel by activityViewModels()
    private var mapList: List<Map> = listOf()
    private var isLeftFromNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel.maps.observe(this) { maps: List<Map>? ->
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
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = FragmentMapListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        adapter = null
        llm = null
    }

    override fun onResume() {
        super.onResume()

        /* When modifications happened outside of the context of this fragment, e.g if a map image
         * was changed in the settings fragment, we need to refresh the view. */
        adapter?.notifyDataSetChanged()

        /* This fragment might be instructed to scroll to a position (by index int he dataset).
         * In case of no instruction, the value of the index is the default -1 value. In this case,
         * we try to restore the previous state. */
        if (args.scrollToPosition != -1) {
            llm?.scrollToPositionWithOffset(args.scrollToPosition, 0)
        } else {
            /* When navigating back to this fragment, the saved state is non-null so let's use it */
            if (llmState != null) {
                llm?.onRestoreInstanceState(llmState)
            }
        }
    }

    /**
     * When navigating out of this fragment, onPause is called.
     * We save the state for eventually restore it later and also set the [isLeftFromNavigation]
     * flag which is useful when the user navigates back to this fragment (we can then redraw the
     * list of maps) */
    override fun onPause() {
        super.onPause()

        isLeftFromNavigation = true
        llmState = llm?.onSaveInstanceState()
    }

    override fun onStart() {
        super.onStart()
        configureMapList()

        /* Only when navigating back to this fragment, redraw the list of maps. */
        if (isLeftFromNavigation) {
            onMapListUpdate(mapList)
            isLeftFromNavigation = false
        }
    }

    private fun configureMapList() {
        val ctx = context
        if (ctx != null) {
            val recyclerView = binding.mapList
            recyclerView.setHasFixedSize(false)
            llm = LinearLayoutManager(ctx)
            recyclerView.layoutManager = llm
            adapter = MapAdapter(mapRepository, this, this, this,
                    this,
                    ctx.getColor(R.color.colorAccent),
                    ctx.getColor(R.color.colorPrimaryTextWhite),
                    ctx.getColor(R.color.colorPrimaryTextBlack),
                    resources)
            recyclerView.adapter = adapter
        }
    }

    /**
     * Navigate to MapViewFragment.
     * Some teasing user might click on several map cards at the same time. To avoid a navigation
     * issue (IllegalArgumentException thrown because we're already at the desired destination),
     * the first attempt wins.
     */
    override fun onMapSelected(map: Map) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mapListFragment) {
            viewModel.setMap(map)
            val action = MapListFragmentDirections.actionMapListFragmentToMapViewFragment()
            navController.navigate(action)
        }
    }

    /**
     * This fragment and its [MapAdapter] need to take action on map list update.
     */
    private fun onMapListUpdate(mapList: List<Map>) {
        this.mapList = mapList
        val adapter = adapter ?: return
        binding.loadingPanel.visibility = View.GONE
        adapter.setMapList(mapList)

        /* If no maps found, suggest to navigate to map creation */
        if (mapList.isEmpty()) {
            binding.emptyMapPanel.visibility = View.VISIBLE
            val btn = binding.buttonGoToMapCreate
            btn.setOnClickListener {
                findNavController().navigate(R.id.action_global_mapCreateFragment)
            }
        } else {
            binding.emptyMapPanel.visibility = View.GONE
        }
    }

    override fun onMapSettings(map: Map) {
        mapRepository.setSettingsMap(map)

        /* Navigate to the MapSettingsFragment*/
        val action = MapListFragmentDirections.actionMapListFragmentToMapSettingsFragment(map.id)
        findNavController().navigate(action)
    }

    override fun onMapDelete(map: Map) {
        val f = ConfirmDeleteDialog.newInstance(map.id)
        val fragmentManager = parentFragmentManager
        f.show(fragmentManager, "delete")
    }

    override fun onMapFavorite(map: Map, formerPosition: Int) {
        viewModel.toggleFavorite(map)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val llm = llm ?: return
        val llmState = llm.onSaveInstanceState()
        outState.putParcelable(llmStateKey, llmState)
    }

    companion object {
        private const val llmStateKey = "llmState"
    }
}