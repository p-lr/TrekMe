package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment

import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.databinding.FragmentWmtsBinding
import com.peterlaurence.trekme.core.repositories.mapcreate.WmtsSourceRepository
import com.peterlaurence.trekme.ui.mapcreate.dialogs.*
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components.WmtsStateful
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.collectWhileResumed
import com.peterlaurence.trekme.util.collectWhileResumedIn
import com.peterlaurence.trekme.util.collectWhileStartedIn
import com.peterlaurence.trekme.viewmodel.mapcreate.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import ovh.plrapps.mapview.api.*
import javax.inject.Inject

/**
 * Displays Google Maps - compatible tile matrix sets.
 * For example :
 *
 * [IGN WMTS](https://geoservices.ign.fr/documentation/geoservices/wmts.html). A `GetCapabilities`
 * request reveals that each level is square area. Here is an example for level 18 :
 * ```
 * <TileMatrix>
 *   <ows:Identifier>18</ows:Identifier>
 *   <ScaleDenominator>2132.7295838497840572</ScaleDenominator>
 *   <TopLeftCorner>
 *     -20037508.3427892476320267 20037508.3427892476320267
 *   </TopLeftCorner>
 *   <TileWidth>256</TileWidth>
 *   <TileHeight>256</TileHeight>
 *   <MatrixWidth>262144</MatrixWidth>
 *   <MatrixHeight>262144</MatrixHeight>
 * </TileMatrix>
 * ```
 * This level correspond to a 256 * 262144 = 67108864 px wide and height area.
 * The `TopLeftCorner` corner contains the WebMercator coordinates. The bottom right corner has
 * implicitly the opposite coordinates.
 * **Beware** that this "level 18" is actually the 19th level (matrix set starts at 0).
 *
 * The same settings can be seen at [USGS WMTS](https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/WMTS/1.0.0/WMTSCapabilities.xml)
 * for the "GoogleMapsCompatible" TileMatrixSet (and not the "default028mm" one).
 *
 * @author P.Laurence on 11/05/18
 */
@AndroidEntryPoint
class WmtsFragment : Fragment() {
    /* We don't provide a non-null equivalent because we use suspend functions which can access this
     * property when it's null */
    private var _binding: FragmentWmtsBinding? = null

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var eventBus: MapCreateEventBus

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var wmtsSourceRepository: WmtsSourceRepository

    private var wmtsSource: WmtsSource? = null

    private val viewModel: WmtsViewModel by navGraphViewModels(R.id.mapCreationGraph) {
        defaultViewModelProviderFactory
    }

    private val onBoardingViewModel: WmtsOnBoardingViewModel by navGraphViewModels(R.id.mapCreationGraph) {
        defaultViewModelProviderFactory
    }

    private val layerIdToResId = mapOf(
        ignPlanv2 to R.string.layer_ign_plan_v2,
        ignScanExpressStd to R.string.layer_ign_scan_express_std,
        ignClassic to R.string.layer_ign_classic,
        ignSatellite to R.string.layer_ign_satellite,
        osmTopo to R.string.layer_osm_topo,
        osmStreet to R.string.layer_osm_street,
        openTopoMap to R.string.layer_osm_opentopo
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = wmtsSourceRepository.wmtsSourceState.value

        /* Listen to position update */
        locationSource.locationFlow.collectWhileResumed(this) { loc ->
            viewModel.onLocationReceived(loc)
        }

        eventBus.showDownloadFormEvent.map {
            showDownloadForm(it)
        }.collectWhileStartedIn(this)

        /* A hack to circumvent a nasty bug causing the AbstractComposeView to be not responsive
         * to touch events at certain state transitions */
        viewModel.wmtsState.map {
            _binding?.googleMapWmtsComposeView?.also {
                it.disposeComposition()
            }
        }.collectWhileResumedIn(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentWmtsBinding.inflate(inflater, container, false)
        _binding = binding
        binding.googleMapWmtsComposeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                TrekMeTheme {
                    WmtsStateful(
                        viewModel,
                        wmtsSourceRepository.wmtsSourceState,
                        ::showPrimaryLayerSelection,
                        ::showLayerOverlay,
                        appEventBus::openDrawer,
                        onBoardingViewModel
                    )
                }
            }
        }

        return binding.root
    }

    private fun showPrimaryLayerSelection() {
        wmtsSource?.also { wmtsSource ->
            val title = getString(R.string.ign_select_layer_title)
            val layers = viewModel.getAvailablePrimaryLayersForSource(wmtsSource)
                ?: return@also
            val activeLayer =
                viewModel.getActivePrimaryLayerForSource(wmtsSource) ?: return@also
            val ids = layers.map { it.id }
            val values = layers.mapNotNull { translateLayerName(it) }
            val selectedValue = translateLayerName(activeLayer) ?: return@also
            val layerSelectDialog =
                LayerSelectDialog.newInstance(title, ids, values, selectedValue)
            layerSelectDialog.show(
                requireActivity().supportFragmentManager,
                "LayerSelectDialog"
            )
        }
    }

    private fun showLayerOverlay() {
        wmtsSource?.also {
            val bundle = LayerOverlayDataBundle(it)
            val action =
                WmtsFragmentDirections.actionGoogleMapWmtsViewFragmentToLayerOverlayFragment(
                    bundle
                )
            findNavController().navigate(action)
        }
    }

    private fun translateLayerName(layer: Layer): String? {
        val res = layerIdToResId[layer.id] ?: return null
        return getString(res)
    }

    private fun showDownloadForm(data: DownloadFormDataBundle) {
        val fm = activity?.supportFragmentManager ?: return
        val wmtsLevelsDialog = if (data.wmtsSource == WmtsSource.IGN) {
            WmtsLevelsDialogIgn.newInstance(data)
        } else {
            WmtsLevelsDialog.newInstance(data)
        }
        wmtsLevelsDialog.show(fm, "fragment")
    }
}

