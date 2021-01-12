package com.peterlaurence.trekme.ui.mapcreate.overlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.providers.layers.ignRoad
import com.peterlaurence.trekme.core.providers.layers.ignSlopes
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding
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

        initLayerRecyclerView()

        val viewModel: LayerOverlayViewModel by viewModels()
        wmtsSource?.also { source ->
            val dataSet = viewModel.getSelectedLayers(source)
        }

        binding.addLayerFab.setOnClickListener {
            wmtsSource?.also {
                val availableLayers = viewModel.getAvailableLayers(it)

                val ids = availableLayers.map { info -> info.id }
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
            eventBus.layerSelectEvent.collect {
                println("overlay defined : $it")
            }
        }

        return binding.root
    }

    private fun initLayerRecyclerView() {
        //TODO
    }

    private fun translateLayerName(layerId: String): String? {
        val res = layerIdToResId[layerId] ?: return null
        return getString(res)
    }
}