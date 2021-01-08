package com.peterlaurence.trekme.ui.mapcreate.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.api.*
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.WmtsSourceBundle
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.*
import com.peterlaurence.trekme.databinding.FragmentWmtsViewBinding
import com.peterlaurence.trekme.repositories.location.Location
import com.peterlaurence.trekme.repositories.location.LocationSource
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.ui.mapcreate.components.AreaLayer
import com.peterlaurence.trekme.ui.mapcreate.components.AreaListener
import com.peterlaurence.trekme.ui.mapcreate.dialogs.LayerSelectDialog
import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import com.peterlaurence.trekme.ui.mapcreate.views.components.PositionMarker
import com.peterlaurence.trekme.util.collectWhileStarted
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.toMapViewTileStreamProvider
import com.peterlaurence.trekme.viewmodel.mapcreate.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var wmtsSource: WmtsSource? = null
    private var mapView: MapView? = null
    private var areaLayer: AreaLayer? = null
    private var positionMarker: PositionMarker? = null
    private val projection = MercatorProjection()
    private var shouldZoomOnPosition = true

    private val viewModel: GoogleMapWmtsViewModel by activityViewModels()
    private val geocodingViewModel: GeocodingViewModel by viewModels()

    private lateinit var area: Area

    /* Size of level 18 */
    private val mapSize = 67108864

    private val tileSize = 256
    private val x0 = -20037508.3427892476320267
    private val y0 = -x0
    private val x1 = -x0
    private val y1 = x0

    private val layerIdToResId = mapOf(
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

        wmtsSource = arguments?.getParcelable<WmtsSourceBundle>(ARG_WMTS_SOURCE)?.wmtsSource

        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mapView?.destroy()
        mapView = null
        areaLayer = null
        positionMarker = null
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWmtsViewBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = _binding ?: return
        binding.fabSave.setOnClickListener { validateArea() }
        binding.fragmentWmtWarningLink.movementMethod = LinkMovementMethod.getInstance()

        initPlaceRecyclerView()
        configureMapView()

        /* Listen to position update */
        locationSource.locationFlow.collectWhileStarted(this@GoogleMapWmtsViewFragment) { loc ->
            onLocationReceived(loc)
        }

        /* Listen to places search results */
        lifecycleScope.launchWhenResumed {
            geocodingViewModel.geoPlaceFlow.collect {
                it?.let {
                    placesAdapter?.setGeoPlaces(it)
                }
            }
        }

        /* Listen to layer selection event */
        lifecycleScope.launchWhenResumed {
            eventBus.layerSelectEvent.collect {
                onLayerDefined(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Hide the app title */
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        /* Clear the existing action menu */
        menu.clear()

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_wmts, menu)

        val layerMenu = menu.findItem(R.id.map_layer_menu_id)
        layerMenu?.isVisible = shouldShowLayerMenu()

        val areaWidget = menu.findItem(R.id.map_area_widget_id)

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
            areaWidget?.isVisible = !hasFocus
            layerMenu?.isVisible = if (hasFocus) false else shouldShowLayerMenu()
            mapView?.visibility = if (hasFocus) View.GONE else View.VISIBLE
            _binding?.placesRecyclerView?.visibility = if (hasFocus) View.VISIBLE else View.GONE
        }

        /* React to place selection */
        lifecycleScope.launch {
            eventBus.paceSelectEvent.collect {
                searchItem.collapseActionView()
                mapView?.visibility = View.VISIBLE
                _binding?.placesRecyclerView?.visibility = View.GONE
                moveToPlace(it)
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.map_area_widget_id -> {
                areaLayer?.detach()
                addAreaLayer()
                _binding?.fabSave?.visibility = View.VISIBLE
            }
            R.id.map_layer_menu_id -> {
                wmtsSource?.also { wmtsSource ->
                    val title = getString(R.string.ign_select_layer_title)
                    val layers = viewModel.getLayersForSource(wmtsSource) ?: return@also
                    val activeLayer = viewModel.getActiveLayerForSource(wmtsSource) ?: return@also
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

    private fun moveToPlace(place: GeoPlace) {
        /* First, we check that this place is in the bounds of the map */
        val wmtsSource = wmtsSource ?: return
        val mapConfiguration = viewModel.getScaleAndScrollConfig(wmtsSource)
        val boundaryConf = mapConfiguration?.filterIsInstance<BoundariesConfig>()?.firstOrNull()
        boundaryConf?.boundingBoxList?.also { boxes ->
            if (!boxes.contains(place.lat, place.lon)) {
                val view = view ?: return
                Snackbar.make(view, getString(R.string.place_outside_of_covered_area), Snackbar.LENGTH_LONG).show()
                return
            }
        }

        /* If it's in the bounds, add a marker */
        val projected = projection.doProjection(place.lat, place.lon) ?: return
        val X = projected[0]
        val Y = projected[1]

        val mapView = mapView ?: return

        val placeMarker = mapView.getMarkerByTag("place_marker")?.also {
            mapView.moveMarker(it, X, Y)
        } ?: makePlaceMarker().also {
            mapView.addMarker(it, X, Y)
        }

        mapView.moveToMarker(placeMarker, 0.5f, true)
    }

    private fun makePlaceMarker(): ImageView {
        return ImageView(context).apply {
            setImageResource(R.drawable.ic_baseline_location_on_48)
            setColorFilter(getColor(context, R.color.colorMarkerStroke))
            alpha = 0.85f
        }
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

    private fun onLayerDefined(layerId: String) {
        val wmtsSource = wmtsSource ?: return

        /* Update the layer preference */
        viewModel.setLayerForSourceFromId(wmtsSource, layerId)

        /* Then re-create the MapView */
        shouldZoomOnPosition = true
        positionMarker = null
        removeMapView()
        configureMapView()
    }

    private fun configureMapView() = lifecycleScope.launch {
        val wmtsSource = wmtsSource ?: return@launch

        /* 0- Show infinite progressbar to the user until we're done testing the tile provider */
        _binding?.progressBarWaiting?.visibility = View.VISIBLE

        /* 1- Create the TileStreamProvider */
        val streamProvider = viewModel.createTileStreamProvider(wmtsSource)
        if (streamProvider == null) {
            showWarningMessage()
            return@launch
        }

        /* 2- Configure the mapView only if the test succeeds */
        val mapConfiguration = viewModel.getScaleAndScrollConfig(wmtsSource)
        val checkResult = checkTileAccessibility(streamProvider)
        try {
            if (!checkResult) {
                showWarningMessage()
                return@launch
            } else {
                addMapView(streamProvider, mapConfiguration)
                hideWarningMessage()
            }
        } catch (e: IllegalStateException) {
            /* Since this can happen anytime during the lifecycle of this fragment, we should be
             * resilient and discard this error */
        }

        /* 3- Hide the progressbar, whatever the outcome */
        val binding = _binding ?: return@launch
        binding.progressBarWaiting.visibility = View.GONE

        /* 4- Scroll to the init position if there is one pre-configured */
        mapConfiguration?.also { config ->
            /* At this point the mapView should be initialized, but we never know.. */
            mapView?.apply {
                config.forEach {
                    if (it is InitScaleAndScrollConfig) {
                        scale = it.scale
                        scrollTo(it.scrollX, it.scrollY)
                    }
                }
            }
        }
    }

    private fun translateLayerName(layer: Layer): String? {
        val res = layerIdToResId[layer.id] ?: return null
        return getString(res)
    }

    /**
     * Simple check whether we are able to download tiles or not.
     */
    private suspend fun checkTileAccessibility(tileStreamProvider: TileStreamProvider): Boolean = withContext(Dispatchers.IO) {
        when (wmtsSource) {
            WmtsSource.IGN -> {
                try {
                    checkIgnProvider(tileStreamProvider)
                } catch (e: Exception) {
                    false
                }
            }
            WmtsSource.IGN_SPAIN -> checkIgnSpainProvider(tileStreamProvider)
            WmtsSource.USGS -> checkUSGSProvider(tileStreamProvider)
            WmtsSource.OPEN_STREET_MAP -> checkOSMProvider(tileStreamProvider)
            WmtsSource.SWISS_TOPO -> checkSwissTopoProvider(tileStreamProvider)
            WmtsSource.ORDNANCE_SURVEY -> checkOrdnanceSurveyProvider(tileStreamProvider)
            null -> false
        }
    }

    private fun showWarningMessage() {
        val binding = _binding ?: return
        binding.fragmentWmtWarning.visibility = View.VISIBLE
        binding.fragmentWmtWarningLink.visibility = View.VISIBLE

        if (wmtsSource == WmtsSource.IGN) {
            binding.fragmentWmtWarning.text = getText(R.string.mapcreate_warning_ign)
        } else {
            binding.fragmentWmtWarning.text = getText(R.string.mapcreate_warning_others)
        }
    }

    private fun hideWarningMessage() {
        val binding = _binding ?: return
        binding.fragmentWmtWarning.visibility = View.GONE
        binding.fragmentWmtWarningLink.visibility = View.GONE
    }

    private fun addMapView(tileStreamProvider: TileStreamProvider, mapConfig: List<Config>? = null) {
        if (_binding == null) return
        val context = this.context ?: return
        val mapView = MapView(context)

        val config = MapViewConfiguration(
                19, mapSize, mapSize, tileSize,
                tileStreamProvider.toMapViewTileStreamProvider()
        ).setWorkerCount(16).apply {
            /* If we're provided a config, apply it */
            mapConfig?.also {
                it.filterIsInstance<ScaleLimitsConfig>().firstOrNull().also { conf ->
                    conf?.minScale?.also { minScale -> setMinScale(minScale) }
                    conf?.maxScale?.also { maxScale -> setMaxScale(maxScale) }
                }
            }
        }
        /* OSM maps renders too small text. As a workaround, we set the magnifying factor to 1. */
        if (wmtsSource == WmtsSource.OPEN_STREET_MAP) {
            config.setMagnifyingFactor(1)
        }

        mapView.configure(config)

        /* Map calibration */
        mapView.defineBounds(x0, y0, x1, y1)

        /* Add the view */
        setMapView(mapView)
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
        this.mapView?.apply {
            id = R.id.tileview_ign_id
            isSaveEnabled = true
        }

        val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        _binding?.root?.addView(mapView, 0, params)
    }

    private fun removeMapView() {
        _binding?.root?.removeViewAt(0)
    }

    private fun addAreaLayer() {
        val mapView = mapView ?: return
        view?.post {
            areaLayer = AreaLayer(requireContext(), object : AreaListener {
                override fun areaChanged(area: Area) {
                    this@GoogleMapWmtsViewFragment.area = area
                }

                override fun hideArea() {
                }

            })
            areaLayer?.attachTo(mapView)
        }
    }

    /**
     * Called when the user validates his area by clicking on the floating action button.
     */
    private fun validateArea() {
        if (this::area.isInitialized) {
            val fm = activity?.supportFragmentManager
            if (fm != null) {
                wmtsSource?.let {
                    val mapConfiguration = viewModel.getScaleAndScrollConfig(it)
                    val levelConf = mapConfiguration?.firstOrNull { conf -> conf is LevelLimitsConfig } as? LevelLimitsConfig
                    val mapSourceBundle = if (levelConf != null) {
                        WmtsSourceBundle(it, levelConf.levelMin, levelConf.levelMax)
                    } else {
                        WmtsSourceBundle(it)
                    }
                    val wmtsLevelsDialog = if (it == WmtsSource.IGN) {
                        WmtsLevelsDialogIgn.newInstance(area, mapSourceBundle)
                    } else {
                        WmtsLevelsDialog.newInstance(area, mapSourceBundle)
                    }
                    wmtsLevelsDialog.show(fm, "fragment")
                }
            }
        }
    }

    private fun onLocationReceived(location: Location) {
        /* If there is no MapView, no need to go further */
        if (mapView == null) return

        val wmtsSource = wmtsSource ?: return

        lifecycleScope.launch {
            /* Project lat/lon off UI thread */
            val projectedValues = withContext(Dispatchers.Default) {
                projection.doProjection(location.latitude, location.longitude)
            }

            /* Update the position */
            if (projectedValues != null) {
                updatePosition(projectedValues[0], projectedValues[1])
                if (shouldZoomOnPosition) {
                    val mapConfiguration = viewModel.getScaleAndScrollConfig(wmtsSource)
                    val boundaryConf = mapConfiguration?.filterIsInstance<BoundariesConfig>()?.firstOrNull()
                    boundaryConf?.boundingBoxList?.also { boxes ->
                        if (boxes.contains(location.latitude, location.longitude)) {
                            centerOnPosition()
                        }
                    }

                    shouldZoomOnPosition = false
                }
            }
        }
    }

    private fun centerOnPosition() {
        val wmtsSource = wmtsSource ?: return
        val positionMarker = positionMarker ?: return
        val mapConfiguration = viewModel.getScaleAndScrollConfig(wmtsSource)
        val scaleConf = mapConfiguration?.filterIsInstance<ScaleForZoomOnPositionConfig>()?.firstOrNull()
        mapView?.moveToMarker(positionMarker, scaleConf?.scale ?: 1f, true)
    }

    /**
     * Update the position on the map. The first time we update the position, we create the
     * [positionMarker].
     *
     * @param x the projected X coordinate
     * @param y the projected Y coordinate
     */
    private fun updatePosition(x: Double, y: Double) {
        val context = context ?: return
        if (positionMarker == null) {
            positionMarker = PositionMarker(context).also {
                mapView?.addMarker(it, x, y, -0.5f, -0.5f)
            }
        } else {
            positionMarker?.also {
                mapView?.moveMarker(it, x, y)
            }
        }
    }
}

private const val ARG_WMTS_SOURCE = "wmtsSource"
