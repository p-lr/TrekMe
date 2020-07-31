package com.peterlaurence.trekme.ui.mapcreate.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.api.MinimumScaleMode
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.providers.bitmap.*
import com.peterlaurence.trekme.core.providers.layers.ignLayers
import com.peterlaurence.trekme.databinding.FragmentWmtsViewBinding
import com.peterlaurence.trekme.service.event.DownloadServiceStatusEvent
import com.peterlaurence.trekme.ui.dialogs.SelectDialog
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.ui.mapcreate.components.AreaLayer
import com.peterlaurence.trekme.ui.mapcreate.components.AreaListener
import com.peterlaurence.trekme.ui.mapcreate.views.components.PositionMarker
import com.peterlaurence.trekme.ui.mapcreate.views.events.LayerSelectEvent
import com.peterlaurence.trekme.viewmodel.common.Location
import com.peterlaurence.trekme.viewmodel.common.LocationViewModel
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.toMapViewTileStreamProvider
import com.peterlaurence.trekme.viewmodel.mapcreate.GoogleMapWmtsViewModel
import com.peterlaurence.trekme.viewmodel.mapcreate.ScaleAndScrollConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

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
 * @author peterLaurence on 11/05/18
 */
@AndroidEntryPoint
class GoogleMapWmtsViewFragment : Fragment() {
    private var _binding: FragmentWmtsViewBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapSource: MapSource
    private var mapView: MapView? = null
    private var areaLayer: AreaLayer? = null
    private var positionMarker: PositionMarker? = null
    private val projection = MercatorProjection()

    private val viewModel: GoogleMapWmtsViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    private lateinit var area: Area

    /* Size of level 18 */
    private val mapSize = 67108864

    private val tileSize = 256
    private val x0 = -20037508.3427892476320267
    private val y0 = -x0
    private val x1 = -x0
    private val y1 = x0

    companion object {
        private const val ARG_MAP_SOURCE = "mapSource"

        @JvmStatic
        fun newInstance(mapSource: MapSourceBundle): GoogleMapWmtsViewFragment {
            val fragment = GoogleMapWmtsViewFragment()
            val args = Bundle()
            args.putParcelable(ARG_MAP_SOURCE, mapSource)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapSource = arguments?.getParcelable<MapSourceBundle>(ARG_MAP_SOURCE)?.mapSource
                ?: MapSource.OPEN_STREET_MAP

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabSave.setOnClickListener { validateArea() }
        binding.fragmentWmtWarningLink.movementMethod = LinkMovementMethod.getInstance()

        configure()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Hide the app title */
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        /* Clear the existing action menu */
        menu.clear()

        /* Fill the new one */
        inflater.inflate(R.menu.menu_fragment_map_create, menu)

        /* Only show the layer menu for IGN France for instance */
        val layerMenu = menu.findItem(R.id.map_layer_menu_id)
        layerMenu.isVisible = when (mapSource) {
            MapSource.IGN -> true
            else -> false
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    @SuppressLint("RestrictedApi")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.map_area_widget_id -> {
                areaLayer?.detach()
                addAreaLayer()
                binding.fabSave.visibility = View.VISIBLE
            }
            R.id.map_layer_menu_id -> {
                val event = LayerSelectEvent(arrayListOf())
                val title = getString(R.string.ign_select_layer_title)
                val values = ignLayers.map { it.publicName }
                val layerPublicName = viewModel.getLayerPublicNameForSource(mapSource)
                val layerSelectDialog =
                        SelectDialog.newInstance(title, values, layerPublicName, event)
                layerSelectDialog.show(
                        requireActivity().supportFragmentManager,
                        "SelectDialog-${event.javaClass.canonicalName}"
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()

        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()

        stopLocationUpdates()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    private fun startLocationUpdates() {
        locationViewModel.startLocationUpdates()
    }

    private fun stopLocationUpdates() {
        locationViewModel.stopLocationUpdates()
    }

    /**
     * Confirm to the user that the download started.
     */
    @Subscribe
    fun onDownloadServiceStatus(e: DownloadServiceStatusEvent) {
        if (e.started) {
            view?.let {
                val snackBar = Snackbar.make(it, R.string.download_confirm, Snackbar.LENGTH_SHORT)
                snackBar.show()
            }
        }
    }

    @Subscribe
    fun onLayerDefined(e: LayerSelectEvent) {
        /* Update the layer preference */
        viewModel.setLayerPublicNameForSource(mapSource, e.getSelection())

        /* Then re-create the MapView */
        removeMapView()
        configure()
    }

    private fun configure() = lifecycleScope.launch {
        /* 0- Show infinite progressbar to the user until we're done testing the tile provider */
        binding.progressBarWaiting.visibility = View.VISIBLE

        /* 1- Create the TileStreamProvider */
        val streamProvider = viewModel.createTileStreamProvider(mapSource)
        if (streamProvider == null) {
            showWarningMessage()
            return@launch
        }

        /* 2- Configure the mapView only if the test succeeds */
        val mapConfiguration = viewModel.getScaleAndScrollConfig(mapSource)
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
        binding.progressBarWaiting.visibility = View.GONE

        /* 4- Scroll to the init position if there is one pre-configured */
        mapConfiguration?.also {
            /* At this point the mapView should be initialized, but we never know.. */
            mapView?.apply {
                scale = it.scale
                scrollTo(it.scrollX, it.scrollY)
            }
        }

        /* 5- Finally, update the current position */
        locationViewModel.getLocationLiveData().observe(viewLifecycleOwner, Observer {
            it?.let {
                onLocationReceived(it)
            }
        })
    }

    /**
     * Simple check whether we are able to download tiles or not.
     */
    private suspend fun checkTileAccessibility(tileStreamProvider: TileStreamProvider): Boolean = withContext(Dispatchers.IO) {
        when (mapSource) {
            MapSource.IGN -> {
                try {
                    checkIgnProvider(tileStreamProvider)
                } catch (e: Exception) {
                    false
                }
            }
            MapSource.IGN_SPAIN -> checkIgnSpainProvider(tileStreamProvider)
            MapSource.USGS -> checkUSGSProvider(tileStreamProvider)
            MapSource.OPEN_STREET_MAP -> checkOSMProvider(tileStreamProvider)
            MapSource.SWISS_TOPO -> checkSwissTopoProvider(tileStreamProvider)
            MapSource.ORDNANCE_SURVEY -> checkOrdnanceSurveyProvider(tileStreamProvider)
        }
    }

    private fun showWarningMessage() {
        binding.fragmentWmtWarning.visibility = View.VISIBLE
        binding.fragmentWmtWarningLink.visibility = View.VISIBLE

        if (mapSource == MapSource.IGN) {
            binding.fragmentWmtWarning.text = getText(R.string.mapcreate_warning_ign)
        } else {
            binding.fragmentWmtWarning.text = getText(R.string.mapcreate_warning_others)
        }
    }

    private fun hideWarningMessage() {
        binding.fragmentWmtWarning.visibility = View.GONE
        binding.fragmentWmtWarningLink.visibility = View.GONE
    }

    private fun addMapView(tileStreamProvider: TileStreamProvider, scaleAndScrollConfig: ScaleAndScrollConfig? = null) {
        val context = this.context ?: return
        val mapView = MapView(context)

        val config = MapViewConfiguration(
                19, mapSize, mapSize, tileSize,
                tileStreamProvider.toMapViewTileStreamProvider()
        ).setWorkerCount(16).apply {
            /* If we're provided a map config, apply it */
            scaleAndScrollConfig?.minScale?.also { minScale ->
                setMinimumScaleMode(MinimumScaleMode.NONE)
                setMinScale(minScale)
            }
        }

        mapView.configure(config)

        /* Map calibration */
        mapView.defineBounds(x0, y0, x1, y1)

        /* Position marker */
        positionMarker = PositionMarker(context)
        mapView.addMarker(positionMarker!!, 0.0, 0.0, -0.5f, -0.5f)

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
        binding.root.addView(mapView, 0, params)
    }

    private fun removeMapView() {
        binding.root.removeViewAt(0)
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
                mapSource.let {
                    val mapConfiguration = viewModel.getScaleAndScrollConfig(mapSource)
                    val mapSourceBundle = if (mapConfiguration != null) {
                        MapSourceBundle(it, mapConfiguration.levelMin, mapConfiguration.levelMax)
                    } else {
                        MapSourceBundle(it)
                    }
                    val wmtsLevelsDialog = if (it == MapSource.IGN) {
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

        /* A Projection is always defined in this case */
        lifecycleScope.launch {
            val projectedValues = withContext(Dispatchers.Default) {
                projection.doProjection(location.latitude, location.longitude)
            }
            if (projectedValues != null) {
                updatePosition(projectedValues[0], projectedValues[1])
            }
        }
    }

    /**
     * Update the position on the map.
     *
     * @param x the projected X coordinate
     * @param y the projected Y coordinate
     */
    private fun updatePosition(x: Double, y: Double) {
        positionMarker?.also {
            mapView?.moveMarker(it, x, y)
        }
    }
}
