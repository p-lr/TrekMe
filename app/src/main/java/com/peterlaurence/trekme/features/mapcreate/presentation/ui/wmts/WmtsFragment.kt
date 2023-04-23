package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.dialogs.*
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.screen.WmtsStateful
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsOnBoardingViewModel
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.WmtsViewModel
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.collectWhileResumed
import dagger.hilt.android.AndroidEntryPoint
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
 * @since 11/05/18
 */
@AndroidEntryPoint
class WmtsFragment : Fragment() {
    @Inject
    lateinit var appEventBus: AppEventBus

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wmtsSource = wmtsSourceRepository.wmtsSourceState.value

        /* Listen to position update */
        locationSource.locationFlow.collectWhileResumed(this) { loc ->
            viewModel.onLocationReceived(loc)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        /* The action bar is managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            hide()
            title = ""
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TrekMeTheme {
                    WmtsStateful(
                        viewModel,
                        onBoardingViewModel,
                        ::showLayerOverlay,
                        appEventBus::openDrawer
                    )
                }
            }
        }
    }

    private fun showLayerOverlay() {
        wmtsSource?.also {
            val bundle = LayerOverlayDataBundle(it)
            val action =
                WmtsFragmentDirections.actionWmtsFragmentToLayerOverlayFragment(
                    bundle
                )
            findNavController().navigate(action)
        }
    }
}

