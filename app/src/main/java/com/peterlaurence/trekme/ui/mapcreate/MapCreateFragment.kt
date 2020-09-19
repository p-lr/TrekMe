package com.peterlaurence.trekme.ui.mapcreate

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.WmtsSourceBundle
import com.peterlaurence.trekme.databinding.FragmentMapCreateBinding
import com.peterlaurence.trekme.ui.mapcreate.MapSourceAdapter.MapSourceSelectionListener
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @author peterLaurence on 08/04/18
 */
class MapCreateFragment : Fragment(), MapSourceSelectionListener {
    private lateinit var wmtsSourceSet: Array<WmtsSource>

    private var _binding: FragmentMapCreateBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)

        /**
         * When the app is in english, put [WmtsSource.USGS] in front.
         * When in french, put [WmtsSource.IGN] in front.
         */
        wmtsSourceSet = WmtsSource.values().sortedBy {
            if (isEnglish(context) && it == WmtsSource.USGS) {
                -1
            } else if (isFrench(context) && it == WmtsSource.IGN) {
                -1
            } else {
                0
            }
        }.toTypedArray()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = MapSourceAdapter(
                wmtsSourceSet, this, context?.getColor(R.color.colorAccent)
                ?: Color.BLUE,
                context?.getColor(R.color.colorPrimaryTextWhite)
                        ?: Color.WHITE, context?.getColor(R.color.colorPrimaryTextBlack)
                ?: Color.BLACK
        )

        /* Item decoration : divider */
        val dividerItemDecoration = DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
        )
        val divider = context?.let { getDrawable(it, R.drawable.divider) }
        if (divider != null) {
            dividerItemDecoration.setDrawable(divider)
        }

        binding.recyclerviewMapCreate.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun showWmtsViewFragment(wmtsSource: WmtsSource) {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.mapCreateFragment) {
            val bundle = WmtsSourceBundle(wmtsSource)
            val action = MapCreateFragmentDirections.actionMapCreateFragmentToGoogleMapWmtsViewFragment(bundle)
            navController.navigate(action)
        }
    }

    override fun onMapSourceSelected(m: WmtsSource) {
        showWmtsViewFragment(m)
    }
}