package com.peterlaurence.trekme.ui.mapcreate

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceCredentials
import com.peterlaurence.trekme.databinding.FragmentMapCreateBinding
import com.peterlaurence.trekme.ui.mapcreate.MapSourceAdapter.MapSourceSelectionListener
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSelectedEvent
import com.peterlaurence.trekme.ui.mapcreate.events.MapSourceSettingsEvent
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench
import org.greenrobot.eventbus.EventBus

/**
 * This fragment is used for displaying available WMTS map sources.
 *
 * @author peterLaurence on 08/04/18
 */
class MapCreateFragment : Fragment(), MapSourceSelectionListener {
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var mapSourceSet: Array<MapSource>

    private var _binding: FragmentMapCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var selectedMapSource: MapSource

    override fun onAttach(context: Context) {
        super.onAttach(context)

        /**
         * When the app is in english, put [MapSource.USGS] in front.
         * When in french, put [MapSource.IGN] in front.
         */
        mapSourceSet = MapSourceCredentials.supportedMapSource.sortedBy {
            if (isEnglish(context) && it == MapSource.USGS) {
                -1
            } else if (isFrench(context) && it == MapSource.IGN) {
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

        binding.nextButton.setOnClickListener {
            EventBus.getDefault().post(MapSourceSelectedEvent(selectedMapSource))
        }

        binding.settingsButton.setOnClickListener {
            EventBus.getDefault().post(MapSourceSettingsEvent(selectedMapSource))
        }

        val viewManager = LinearLayoutManager(context)
        viewAdapter = MapSourceAdapter(
            mapSourceSet, this, context?.getColor(R.color.colorAccent)
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
        val divider = this.context?.getDrawable(R.drawable.divider)
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

    /**
     * For instance, settings are only relevant for [MapSource.IGN] provider.
     */
    private fun setButtonsAvailability(m: MapSource) {
        binding.nextButton.visibility = View.VISIBLE

        binding.settingsButton.visibility = if (m == MapSource.IGN) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    override fun onMapSourceSelected(m: MapSource) {
        selectedMapSource = m
        setButtonsAvailability(m)
    }
}