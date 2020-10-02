package com.peterlaurence.trekme.ui.mapview.components.markermanage

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.databinding.FragmentMarkerManageBinding
import com.peterlaurence.trekme.viewmodel.markermanage.GeographicCoords
import com.peterlaurence.trekme.viewmodel.markermanage.MakerManageViewModel
import com.peterlaurence.trekme.viewmodel.markermanage.ProjectedCoords

/**
 * A [Fragment] subclass that provides tools to:
 *
 *  * Edit a marker's name
 *  * Edit a marker's associated comment
 *  * See and edit WGS84 (and projected coordinates, if applicable)
 *
 * When the latitude or longitude is changed, the projected coordinates are changed accordingly, if
 * the map has a projection. Right after this change, this do not trigger un update of lat and lon,
 * since this would result in a infinite loop.
 *
 * The reverse applies: when projected coordinates are changes, latitude and longitude are updated
 * only once.
 *
 * @author P.Laurence on 23/04/2017 -- Converted to Kotlin on 24/09/2019
 */
class MarkerManageFragment : Fragment() {
    private var _binding: FragmentMarkerManageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MakerManageViewModel by viewModels()
    private val args: MarkerManageFragmentArgs by navArgs()

    private var map: Map? = null
    private var marker: MarkerGson.Marker? = null

    /*
     * Be VERY careful not to break the logic of those flags, since their purpose is to prevent
     * infinite modification loop (geo_coord change -> proj_coord change -> geo_coord change -> ...)
     */
    private var infiniteLoopGuardGeo = false
    private var infiniteLoopGuardProj = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        viewModel.getGeographicLiveData().observe(this, Observer<GeographicCoords> {
            it?.let {
                binding.markerLonId.setText("${it.lon}")
                binding.markerLatId.setText("${it.lat}")
            }
            infiniteLoopGuardGeo = false
        })

        viewModel.getProjectedLiveData().observe(this, Observer<ProjectedCoords> {
            it?.let {
                binding.markerProjXId.setText("${it.X}")
                binding.markerProjYId.setText("${it.Y}")
            }
            infiniteLoopGuardProj = false
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentMarkerManageBinding.inflate(inflater, container, false)

        map = MapLoader.getMap(args.mapId)
        map?.markers?.firstOrNull { it == args.marker }?.let {
            this.marker = it
        }

        binding.markerLatId.addTextChangedListener(
                onTextChanged = { _, _, _, _ ->
                    if (!infiniteLoopGuardGeo) {
                        onGeographicCoordsChanged()
                    }
                }
        )

        binding.markerLonId.addTextChangedListener(
                onTextChanged = { _, _, _, _ ->
                    if (!infiniteLoopGuardGeo) {
                        onGeographicCoordsChanged()
                    }
                }
        )

        binding.markerProjXId.addTextChangedListener(
                onTextChanged = { _, _, _, _ ->
                    if (!infiniteLoopGuardProj) {
                        onProjectedCoordsChanged()
                    }
                }
        )

        binding.markerProjYId.addTextChangedListener(
                onTextChanged = { _, _, _, _ ->
                    if (!infiniteLoopGuardProj) {
                        onProjectedCoordsChanged()
                    }
                }
        )

        updateView()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        /* Hide the app title */
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayShowTitleEnabled(false)

        /* Clear the existing action menu */
        menu.clear()

        /* .. and fill the new one */
        inflater.inflate(R.menu.menu_fragment_marker_manage, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_marker_id -> {
                saveChanges()
                true
            }
            R.id.undo_marker_id -> {
                updateView()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onGeographicCoordsChanged() {
        if (infiniteLoopGuardProj) {
            return
        }
        map?.let {
            var lat: Double? = null
            var lon: Double? = null
            try {
                lat = binding.markerLatId.text?.toString()?.toDouble()
                lon = binding.markerLonId.text?.toString()?.toDouble()
            } catch (e: Exception) {
            }

            if (lat != null && lon != null) {
                infiniteLoopGuardProj = true
                viewModel.onGeographicValuesChanged(it, lat, lon)
            }
        }
    }

    private fun onProjectedCoordsChanged() {
        if (infiniteLoopGuardGeo) {
            return
        }
        map?.let {
            var X: Double? = null
            var Y: Double? = null
            try {
                X = binding.markerProjXId.text?.toString()?.toDouble()
                Y = binding.markerProjYId.text?.toString()?.toDouble()
            } catch (e: Exception) {
            }

            if (X != null && Y != null) {
                infiniteLoopGuardGeo = true
                viewModel.onProjectedCoordsChanged(it, X, Y)
            }
        }
    }

    private fun updateView() {
        val marker = this.marker ?: return
        binding.markerNameId.setText(marker.name)
        binding.markerLatId.setText(marker.lat.toString())
        binding.markerLonId.setText(marker.lon.toString())
        binding.markerCommentId.setText(marker.comment)

        /* Check whether projected coordinates fields should be shown or not */
        if (map?.projection == null) {
            binding.markerProjLabelId.visibility = GONE
            binding.markerProjXId.visibility = GONE
            binding.markerProjYId.visibility = GONE
            return
        }

        binding.markerProjXId.setText(marker.proj_x.toString())
        binding.markerProjYId.setText(marker.proj_y.toString())
    }

    private fun saveChanges() {
        val marker = this.marker ?: return
        try {
            marker.lat = java.lang.Double.valueOf(binding.markerLatId.text.toString())
            marker.lon = java.lang.Double.valueOf(binding.markerLonId.text.toString())
            marker.proj_x = java.lang.Double.valueOf(binding.markerProjXId.text.toString())
            marker.proj_y = java.lang.Double.valueOf(binding.markerProjYId.text.toString())
        } catch (e: Exception) {
            //don't care
        }

        marker.name = binding.markerNameId.text.toString()
        marker.comment = binding.markerCommentId.text.toString()

        /* Save the changes on the markers.json file */
        map?.let { MapLoader.saveMarkers(it) }

        hideSoftKeyboard()

        /* Show a snackbar to confirm the changes and offer to show the map */
        val snackbar = Snackbar.make(binding.root, R.string.marker_changes_saved, Snackbar.LENGTH_SHORT)
        snackbar.setAction(R.string.show_map_action) {
            findNavController().navigate(R.id.mapViewFragment)
        }
        snackbar.show()
    }

    private fun hideSoftKeyboard() {
        val activity = activity ?: return

        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}