package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.ignRoad
import com.peterlaurence.trekme.core.providers.layers.ignSlopes
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties
import com.peterlaurence.trekme.ui.mapcreate.dialogs.LayerSelectDialog
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.viewmodel.mapcreate.LayerOverlayViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class LayerOverlayFragment : Fragment() {
    @Inject
    lateinit var eventBus: MapCreateEventBus

    private var _binding: FragmentLayerOverlayBinding? = null
    private var wmtsSource: WmtsSource? = null

    private val layerIdToResId = mapOf(
            ignRoad to R.string.layer_ign_roads,
            ignSlopes to R.string.layer_ign_slopes,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = arguments?.let {
            LayerOverlayFragmentArgs.fromBundle(it)
        }?.wmtsSourceBundle?.wmtsSource
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentLayerOverlayBinding.inflate(inflater, container, false)
        _binding = binding

        /* Init recycler view and adapter */
        val recyclerView = binding.recyclerView
        val llm = LinearLayoutManager(context)
        recyclerView.layoutManager = llm

        val viewModel: LayerOverlayViewModel by viewModels()
        val wmtsSource = wmtsSource ?: return binding.root
        val itemTouchHelper = makeItemTouchHelper(viewModel, wmtsSource)
        itemTouchHelper.attachToRecyclerView(recyclerView)
        val adapter = LayerOverlayAdapter(itemTouchHelper)
        recyclerView.adapter = adapter
        viewModel.init(wmtsSource)

        viewModel.liveData.observe(viewLifecycleOwner) { properties ->
            if (properties.isNullOrEmpty()) {
                binding.header.visibility = View.GONE
                binding.emptyMessage.visibility = View.VISIBLE
            } else {
                binding.header.visibility = View.VISIBLE
                binding.emptyMessage.visibility = View.GONE
                val dataSet = properties.mapNotNull { property ->
                    val name = translateLayerName(property.layer.id) ?: return@mapNotNull null
                    LayerInfo(name, property)
                }
                adapter.setLayerInfo(dataSet)
            }
        }

        binding.addLayerFab.setOnClickListener {
            val ids = viewModel.getAvailableLayersId(wmtsSource)
            val values = ids.mapNotNull { id -> translateLayerName(id) }
            val layerSelectDialog =
                    LayerSelectDialog.newInstance("Select a layer", ids, values, "")
            layerSelectDialog.show(
                    requireActivity().supportFragmentManager,
                    "LayerOverlaySelectDialog"
            )
        }

        /* Listen to layer selection event */
        lifecycleScope.launchWhenResumed {
            eventBus.layerSelectEvent.collect { id ->
                viewModel.addLayer(wmtsSource, id)
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun makeItemTouchHelper(viewModel: LayerOverlayViewModel, wmtsSource: WmtsSource): ItemTouchHelper {
        val simpleTouchCallback = object : ItemTouchHelper.SimpleCallback(
                UP or DOWN or LEFT or RIGHT, LEFT or RIGHT) {
            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                viewModel.moveLayer(wmtsSource, from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                viewModel.removeLayer(wmtsSource, viewHolder.adapterPosition)
            }
        }

        return ItemTouchHelper(simpleTouchCallback)
    }

    private fun translateLayerName(layerId: String): String? {
        val res = layerIdToResId[layerId] ?: return null
        return getString(res)
    }
}

data class LayerInfo(val name: String, val properties: LayerProperties)