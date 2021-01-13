package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.ignRoad
import com.peterlaurence.trekme.core.providers.layers.ignSlopes
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding
import com.peterlaurence.trekme.ui.mapcreate.dialogs.LayerSelectDialog
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.viewmodel.mapcreate.LayerOverlayViewModel
import com.peterlaurence.trekme.viewmodel.mapcreate.LayerProperties
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
        val adapter = LayerOverlayAdapter()
        recyclerView.adapter = adapter

        val viewModel: LayerOverlayViewModel by viewModels()
        fun updateLayers() {
            wmtsSource?.also { source ->
                val properties = viewModel.getSelectedLayers(source)
                val dataSet = properties.mapNotNull {
                    val name = translateLayerName(it.layer.id) ?: return@mapNotNull null
                    LayerInfo(name, it)
                }
                adapter.setLayerInfo(dataSet)
            }
        }
        updateLayers()

        binding.addLayerFab.setOnClickListener {
            wmtsSource?.also {
                val ids = viewModel.getAvailableLayersId(it)

                val values = ids.mapNotNull { id -> translateLayerName(id) }
                val layerSelectDialog =
                        LayerSelectDialog.newInstance("Select a layer", ids, values, "")
                layerSelectDialog.show(
                        requireActivity().supportFragmentManager,
                        "LayerOverlaySelectDialog"
                )
            }
        }

        /* Listen to layer selection event */
        lifecycleScope.launchWhenResumed {
            eventBus.layerSelectEvent.collect { id ->
                wmtsSource?.also { source ->
                    viewModel.addLayer(source, id)
                    updateLayers()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun translateLayerName(layerId: String): String? {
        val res = layerIdToResId[layerId] ?: return null
        return getString(res)
    }
}

data class LayerInfo(val name: String, val properties: LayerProperties)