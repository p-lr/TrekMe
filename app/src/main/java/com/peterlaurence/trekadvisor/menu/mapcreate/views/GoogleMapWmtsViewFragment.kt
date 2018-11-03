package com.peterlaurence.trekadvisor.menu.mapcreate.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.*
import com.peterlaurence.trekadvisor.R
import com.peterlaurence.trekadvisor.core.mapsource.MapSource
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceBundle
import com.peterlaurence.trekadvisor.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderIgn
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderIgnSpain
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderOSM
import com.peterlaurence.trekadvisor.core.providers.BitmapProviderUSGS
import com.peterlaurence.trekadvisor.core.providers.layers.IgnLayers
import com.peterlaurence.trekadvisor.menu.dialogs.SelectDialog
import com.peterlaurence.trekadvisor.menu.mapcreate.components.Area
import com.peterlaurence.trekadvisor.menu.mapcreate.components.AreaLayer
import com.peterlaurence.trekadvisor.menu.mapcreate.components.AreaListener
import com.peterlaurence.trekadvisor.menu.mapcreate.views.events.LayerSelectEvent
import com.peterlaurence.trekadvisor.menu.mapview.TileViewExtended
import com.peterlaurence.trekadvisor.model.LayerForSource
import com.peterlaurence.trekadvisor.service.event.DownloadServiceStatusEvent
import com.qozix.tileview.TileView
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.widgets.ZoomPanLayout
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
 *
 * The same settings can be seen at [USGS WMTS](https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/WMTS/1.0.0/WMTSCapabilities.xml)
 * for the "GoogleMapsCompatible" TileMatrixSet (and not the "default028mm" one).
 *
 * @author peterLaurence on 11/05/18
 */
class GoogleMapWmtsViewFragment : Fragment() {
    private lateinit var mapSource: MapSource
    private lateinit var rootView: ConstraintLayout
    private lateinit var tileView: TileViewExtended
    private lateinit var areaLayer: AreaLayer
    private lateinit var saveFab: FloatingActionButton

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapSource = arguments?.getParcelable<MapSourceBundle>(ARG_MAP_SOURCE)?.mapSource ?: MapSource.OPEN_STREET_MAP

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_ign_view, container, false) as ConstraintLayout

        /* Configure the floating action button */
        saveFab = rootView.findViewById(R.id.fab_save)
        saveFab.setOnClickListener { validateArea() }

        createTileView()

        return rootView
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
                saveFab.visibility = View.VISIBLE
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

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
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

        /* The re-create the tileview */
        removeTileView()
        createTileView()
    }

    private fun createTileView() {
        val layerRealName = LayerForSource.resolveLayerName(mapSource)
        val bitmapProvider = createBitmapProvider(layerRealName)
        addTileView(bitmapProvider)
    }

    private fun addTileView(bitmapProvider: BitmapProvider?) {
        val tileView = TileViewExtended(this.context)

        /* IGN wmts maps are square */
        tileView.setSize(mapSize, mapSize)

        /* We will display levels 1..18 */
        val levelCount = highestLevel
        val minScale = 1 / Math.pow(2.0, (levelCount - 1).toDouble()).toFloat()

        /* Scale limits */
        tileView.setScaleLimits(minScale, 1f)

        /* Starting scale */
        tileView.scale = minScale

        /* DetailLevel definition */
        for (level in 0 until levelCount) {
            /* Calculate each level scale for best precision */
            val scale = 1 / Math.pow(2.0, (levelCount - level - 1).toDouble()).toFloat()

            tileView.addDetailLevel(scale, level + 1, tileSize, tileSize)
        }

        /* Allow the scale to be no less to see the entire map */
        tileView.setMinimumScaleMode(ZoomPanLayout.MinimumScaleMode.FIT)

        /* Render while panning */
        tileView.setShouldRenderWhilePanning(true)

        /* Map calibration */
        setTileViewBounds(tileView)

        /* The BitmapProvider */
        tileView.setBitmapProvider(bitmapProvider)

        /* Add the view */
        setTileView(tileView)
    }

    private fun setTileViewBounds(tileView: TileView) {
        tileView.defineBounds(x0, y0, x1, y1)
    }

    private fun setTileView(tileView: TileViewExtended) {
        this.tileView = tileView
        this.tileView.id = R.id.tileview_ign_id
        this.tileView.isSaveEnabled = true
        rootView.addView(tileView, 0)
    }

    private fun createBitmapProvider(layer: String): BitmapProvider? {
        return when (mapSource) {
            MapSource.IGN -> {
                val ignCredentials = MapSourceCredentials.getIGNCredentials()!!
                if (layer.isNotEmpty()) {
                    BitmapProviderIgn(ignCredentials, layer)
                } else {
                    BitmapProviderIgn(ignCredentials)
                }
            }
            MapSource.USGS -> BitmapProviderUSGS()
            MapSource.OPEN_STREET_MAP -> BitmapProviderOSM()
            MapSource.IGN_SPAIN -> BitmapProviderIgnSpain()
        }
    }

    private fun removeTileView() {
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
            areaLayer.attachTo(tileView)
        }
    }

    /**
     * Called when the user validates his area by clicking on the floating action button.
     */
    private fun validateArea() {
        if (this::area.isInitialized) {
            val fm = activity?.supportFragmentManager
            mapSource.let {
                val wmtsLevelsDialog = WmtsLevelsDialog.newInstance(area, MapSourceBundle(it))
                wmtsLevelsDialog.show(fm, "fragment")
            }
        }
    }
}
