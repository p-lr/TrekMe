package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.databinding.FragmentWmtsViewBinding
import com.peterlaurence.trekme.repositories.mapcreate.WmtsSourceRepository
import com.peterlaurence.trekme.ui.mapcreate.dialogs.*
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components.GoogleMapWmts
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.collectWhileResumed
import com.peterlaurence.trekme.util.collectWhileResumedIn
import com.peterlaurence.trekme.util.collectWhileStartedIn
import com.peterlaurence.trekme.viewmodel.mapcreate.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
class GoogleMapWmtsViewFragment : Fragment() {
    /* We don't provide a non-null equivalent because we use suspend functions which can access this
     * property when it's null */
    private var _binding: FragmentWmtsViewBinding? = null

    @Inject
    lateinit var eventBus: MapCreateEventBus

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var wmtsSourceRepository: WmtsSourceRepository

    private var wmtsSource: WmtsSource? = null

    private val viewModel: GoogleMapWmtsViewModel by activityViewModels()
    private val geocodingViewModel: GeocodingViewModel by viewModels()

    private val layerIdToResId = mapOf(
        ignPlanv2 to R.string.layer_ign_plan_v2,
        ignScanExpressStd to R.string.layer_ign_scan_express_std,
        ignClassic to R.string.layer_ign_classic,
        ignSatellite to R.string.layer_ign_satellite,
        osmTopo to R.string.layer_osm_topo,
        osmStreet to R.string.layer_osm_street,
        openTopoMap to R.string.layer_osm_opentopo
    )

    private var placesAdapter: PlacesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = wmtsSourceRepository.wmtsSourceState.value

        setHasOptionsMenu(true)

        /* Listen to position update */
        locationSource.locationFlow.collectWhileResumed(this) { loc ->
            viewModel.onLocationReceived(loc)
        }

        /* Listen to places search results */
        geocodingViewModel.geoPlaceFlow.collectWhileResumed(this) {
            it?.let {
                placesAdapter?.setGeoPlaces(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        placesAdapter = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentWmtsViewBinding.inflate(inflater, container, false)
        _binding = binding
        binding.googleMapWmtsComposeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                TrekMeTheme {
                    GoogleMapWmts(viewModel)
                }
            }
        }

        initPlaceRecyclerView()

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Clear the existing action menu */
        menu.clear()

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_wmts, menu)

        val layerMenu = menu.findItem(R.id.map_layer_menu_id)
        layerMenu?.isVisible = shouldShowLayerMenu()

        val layerOverlayMenu = menu.findItem(R.id.overlay_layers_id)
        layerOverlayMenu?.isVisible = shouldShowLayerOverlayMenu()

        val zoomOnPosItem = menu.findItem(R.id.zoom_on_position_id)

        val searchItem = menu.findItem(R.id.search) ?: return
        val searchView = searchItem.actionView as SearchView

        val queryListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    geocodingViewModel.search(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    geocodingViewModel.search(newText)
                }
                return true
            }
        }
        searchView.setOnQueryTextListener(queryListener)
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            zoomOnPosItem?.isVisible = !hasFocus
            layerMenu?.isVisible = if (hasFocus) false else shouldShowLayerMenu()
            _binding?.placesRecyclerView?.visibility = if (hasFocus) View.VISIBLE else View.GONE
            viewModel.onGeocodingSearchFocusChange(hasFocus)
        }

        /* React to place selection */
        lifecycleScope.launch {
            eventBus.placeSelectEvent.collect {
                searchItem.collapseActionView()
                _binding?.placesRecyclerView?.visibility = View.GONE
            }
        }

        eventBus.showDownloadFormEvent.map {
            showDownloadForm(it)
        }.collectWhileStartedIn(this)

        /* A hack to circumvent a nasty bug causing the AbstractComposeView to be not responsive
         * to touch events at certain state transitions */
        viewModel.state.map {
            _binding?.googleMapWmtsComposeView?.also {
                it.disposeComposition()
                it.createComposition()
            }
        }.collectWhileResumedIn(this)

        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.zoom_on_position_id -> {
                viewModel.zoomOnPosition()
            }
            R.id.map_layer_menu_id -> {
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
            R.id.overlay_layers_id -> {
                wmtsSource?.also {
                    val bundle = LayerOverlayDataBundle(it)
                    val action =
                        GoogleMapWmtsViewFragmentDirections.actionGoogleMapWmtsViewFragmentToLayerOverlayFragment(
                            bundle
                        )
                    findNavController().navigate(action)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initPlaceRecyclerView() {
        val context = context ?: return
        val binding = _binding ?: return
        val llm = LinearLayoutManager(context)
        val recyclerView = binding.placesRecyclerView
        recyclerView.layoutManager = llm

        placesAdapter = PlacesAdapter(eventBus)
        recyclerView.adapter = placesAdapter
    }

    /**
     * Only show the layer menu for IGN France and OSM
     */
    private fun shouldShowLayerMenu(): Boolean {
        return when (wmtsSource) {
            WmtsSource.IGN -> true
            WmtsSource.OPEN_STREET_MAP -> true
            else -> false
        }
    }

    private fun shouldShowLayerOverlayMenu(): Boolean {
        return wmtsSource == WmtsSource.IGN
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

