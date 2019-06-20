package com.peterlaurence.trekme.ui.mapcreate.views

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.mapview.MapView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceBundle
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.core.providers.bitmap.checkIgnProvider
import com.peterlaurence.trekme.core.providers.bitmap.checkIgnSpainProvider
import com.peterlaurence.trekme.core.providers.bitmap.checkOSMProvider
import com.peterlaurence.trekme.core.providers.bitmap.checkUSGSProvider
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.model.providers.layers.LayerForSource
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderIgn
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderIgnSpain
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderOSM
import com.peterlaurence.trekme.model.providers.stream.TileStreamProviderUSGS
import com.peterlaurence.trekme.service.event.DownloadServiceStatusEvent
import com.peterlaurence.trekme.ui.dialogs.SelectDialog
import com.peterlaurence.trekme.ui.mapcreate.components.Area
import com.peterlaurence.trekme.ui.mapcreate.components.AreaLayer
import com.peterlaurence.trekme.ui.mapcreate.components.AreaListener
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSettingsEvent
import com.peterlaurence.trekme.ui.mapcreate.views.events.LayerSelectEvent
import com.peterlaurence.trekme.viewmodel.common.tileviewcompat.toMapViewTileStreamProvider
import kotlinx.android.synthetic.main.fragment_wmts_view.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import kotlin.coroutines.CoroutineContext

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
 *
 * The same settings can be seen at [USGS WMTS](https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/WMTS/1.0.0/WMTSCapabilities.xml)
 * for the "GoogleMapsCompatible" TileMatrixSet (and not the "default028mm" one).
 *
 * @author peterLaurence on 11/05/18
 */
class GoogleMapWmtsViewFragment : Fragment(), CoroutineScope {
    private lateinit var job: Job
    private lateinit var mapSource: MapSource
    private lateinit var rootView: ConstraintLayout
    private lateinit var mapView: MapView
    private lateinit var areaLayer: AreaLayer

    private lateinit var area: Area

    /* Size of level 18 */
    private val mapSize = 67108864
    private val highestLevel = 18

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

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapSource = arguments?.getParcelable<MapSourceBundle>(ARG_MAP_SOURCE)?.mapSource
                ?: MapSource.OPEN_STREET_MAP

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_wmts_view, container, false) as ConstraintLayout

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fabSave.setOnClickListener { validateArea() }
        fragmentWmtWarningLink.movementMethod = LinkMovementMethod.getInstance()

        /**
         * If there is something wrong with IGN credentials, a special button helps to go directly
         * to the credentials editing fragment.
         */
        fragmentWmtsNagivateToIgnCredentials.setOnClickListener {
            EventBus.getDefault().post(MapSourceSettingsEvent(MapSource.IGN))
        }

        createMapView()
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
                if (this::areaLayer.isInitialized) {
                    areaLayer.detach()
                }
                addAreaLayer()
                fabSave.visibility = View.VISIBLE
            }
            R.id.map_layer_menu_id -> {
                val event = LayerSelectEvent(arrayListOf())
                val title = getString(R.string.ign_select_layer_title)
                val values = IgnLayers.values().map { it.publicName }
                val layerPublicName = LayerForSource.getLayerPublicNameForSource(mapSource)
                val layerSelectDialog = SelectDialog.newInstance(title, values, layerPublicName, event)
                layerSelectDialog.show(activity!!.supportFragmentManager, "SelectDialog-${event.javaClass.canonicalName}")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        job = Job()
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        job.cancel()
        super.onStop()
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
        LayerForSource.setLayerPublicNameForSource(mapSource, e.getSelection())

        /* The re-create the mapview */
        removeMapView()
        createMapView()
    }

    private fun createMapView() {
        checkTileAccessibility()
        val layerRealName = LayerForSource.resolveLayerName(mapSource)
        val streamProvider = createTileStreamProvider(layerRealName)
        addMapView(streamProvider)
    }

    /**
     * Simple check whether we are able to download tiles or not.
     * If not, display a warning.
     */
    private fun CoroutineScope.checkTileAccessibility(): Job = launch {
        async(Dispatchers.IO) {
            return@async when (mapSource) {
                MapSource.IGN -> {
                    try {
                        val ignCredentials = MapSourceCredentials.getIGNCredentials()!!
                        checkIgnProvider(ignCredentials.api!!, ignCredentials.user!!, ignCredentials.pwd!!)
                    } catch (e: Exception) {
                        false
                    }
                }
                MapSource.IGN_SPAIN -> checkIgnSpainProvider()
                MapSource.USGS -> checkUSGSProvider()
                MapSource.OPEN_STREET_MAP -> checkOSMProvider()
            }
        }.await().also {
            if (!it) {
                showWarningMessage()
            } else {
                hideWarningMessage()
            }
        }
    }

    private fun showWarningMessage() {
        fragmentWmtWarning.visibility = View.VISIBLE
        fragmentWmtsNagivateToIgnCredentials.visibility = View.VISIBLE
        fragmentWmtWarningLink.visibility = View.VISIBLE

        if (mapSource == MapSource.IGN) {
            fragmentWmtWarning.text = getText(R.string.mapcreate_warning_ign)
        } else {
            fragmentWmtWarning.text = getText(R.string.mapcreate_warning_others)
        }
    }

    private fun hideWarningMessage() {
        fragmentWmtWarning.visibility = View.GONE
        fragmentWmtsNagivateToIgnCredentials.visibility = View.GONE
        fragmentWmtWarningLink.visibility = View.GONE
    }

    private fun addMapView(tileStreamProvider: TileStreamProvider) {
        val mapView = MapView(this.context!!)

        mapView.configure(highestLevel, mapSize, mapSize, tileSize, tileStreamProvider.toMapViewTileStreamProvider())

        /* Map calibration */
        mapView.defineBounds(x0, y0, x1, y1)

        /* Add the view */
        setMapView(mapView)
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
        this.mapView.id = R.id.tileview_ign_id
        this.mapView.isSaveEnabled = true
        val params = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        rootView.addView(mapView, 0, params)
    }

    /**
     * TODO: this should be the responsibility of a view-model
     */
    private fun createTileStreamProvider(layer: String): TileStreamProvider {
        return when (mapSource) {
            MapSource.IGN -> {
                val ignCredentials = MapSourceCredentials.getIGNCredentials()!!
                if (layer.isNotEmpty()) {
                    TileStreamProviderIgn(ignCredentials, layer)
                } else {
                    TileStreamProviderIgn(ignCredentials)
                }
            }
            MapSource.USGS -> TileStreamProviderUSGS()
            MapSource.OPEN_STREET_MAP -> TileStreamProviderOSM()
            MapSource.IGN_SPAIN -> TileStreamProviderIgnSpain()
        }
    }

    private fun removeMapView() {
        rootView.removeViewAt(0)
    }

    private fun addAreaLayer() {
        view?.post {
            areaLayer = AreaLayer(context!!, object : AreaListener {
                override fun areaChanged(area: Area) {
                    this@GoogleMapWmtsViewFragment.area = area
                }

                override fun hideArea() {
                }

            })
            areaLayer.attachTo(mapView)
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
                    val wmtsLevelsDialog = WmtsLevelsDialog.newInstance(area, MapSourceBundle(it))
                    wmtsLevelsDialog.show(fm, "fragment")
                }
            }
        }
    }
}
