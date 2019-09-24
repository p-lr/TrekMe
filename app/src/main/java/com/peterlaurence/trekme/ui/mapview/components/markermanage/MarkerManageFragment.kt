package com.peterlaurence.trekme.ui.mapview.components.markermanage

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader

/**
 * A [Fragment] subclass that provides tools to :
 *
 *  * Edit a marker's associated comment
 *  * See the WGS84 and projected coordinates of the marker, if possible
 *  * Delete the marker
 *
 *
 * @author peterLaurence on 23/04/2017 -- Converted to Kotlin on 24/09/2019
 */
class MarkerManageFragment : Fragment() {
    private lateinit var rootView: View
    private var markerManageFragmentInteractionListener: MarkerManageFragmentInteractionListener? = null

    private var map: Map? = null
    private var marker: MarkerGson.Marker? = null

    private var nameEditText: TextInputEditText? = null
    private var latEditText: TextInputEditText? = null
    private var lonEditText: TextInputEditText? = null
    private var projectionLabel: TextView? = null
    private var projectionX: TextInputEditText? = null
    private var projectionY: TextInputEditText? = null
    private var comment: EditText? = null

    interface MarkerManageFragmentInteractionListener {
        fun showCurrentMap()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MarkerManageFragmentInteractionListener) {
            markerManageFragmentInteractionListener = context
        } else {
            throw RuntimeException("$context must implement MarkerManageFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        rootView = inflater.inflate(R.layout.fragment_marker_manage, container, false)

        /* The view fields */
        nameEditText = rootView.findViewById(R.id.marker_name_id)
        latEditText = rootView.findViewById(R.id.marker_lat_id)
        lonEditText = rootView.findViewById(R.id.marker_lon_id)
        projectionLabel = rootView.findViewById(R.id.marker_proj_label_id)
        projectionX = rootView.findViewById(R.id.marker_proj_x_id)
        projectionY = rootView.findViewById(R.id.marker_proj_y_id)
        comment = rootView.findViewById(R.id.marker_comment_id)

        val args = arguments
        if (args != null) {
            val mapId = args.get(MAP_ID) as Int
            map = MapLoader.getMap(mapId)
            val marker = args.get(MARKER_ID) as MarkerGson.Marker
            map?.markers?.firstOrNull { it == marker }?.let {
                this.marker = it
            }
        }

        updateView()
        return rootView
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

    private fun updateView() {
        val marker = this.marker ?: return
        nameEditText?.setText(marker.name)
        latEditText?.setText(marker.lat.toString())
        lonEditText?.setText(marker.lon.toString())
        comment?.setText(marker.comment)

        /* Check whether projected coordinates fields should be shown or not */
        if (map?.projection == null) {
            projectionLabel?.visibility = GONE
            projectionX?.visibility = GONE
            projectionY?.visibility = GONE
            return
        }

        projectionX?.setText(marker.proj_x.toString())
        projectionY?.setText(marker.proj_y.toString())
    }

    private fun saveChanges() {
        val marker = this.marker ?: return
        try {
            marker.lat = java.lang.Double.valueOf(latEditText!!.text.toString())
            marker.lon = java.lang.Double.valueOf(lonEditText!!.text.toString())
            marker.proj_x = java.lang.Double.valueOf(projectionX!!.text.toString())
            marker.proj_y = java.lang.Double.valueOf(projectionY!!.text.toString())
        } catch (e: Exception) {
            //don't care
        }

        marker.name = nameEditText?.text.toString()
        marker.comment = comment?.text.toString()

        /* Save the changes on the markers.json file */
        map?.let { MapLoader.saveMarkers(it) }

        hideSoftKeyboard()

        /* Show a snackbar to confirm the changes and offer to show the map */
        val snackbar = Snackbar.make(rootView, R.string.marker_changes_saved, Snackbar.LENGTH_SHORT)
        snackbar.setAction(R.string.show_map_action) { markerManageFragmentInteractionListener!!.showCurrentMap() }
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

    companion object {
        const val MAP_ID = "MAP_ID"
        const val MARKER_ID = "MARKER_ID"

        fun newInstance(mapId: Int, marker: MarkerGson.Marker): Fragment {
            val f = MarkerManageFragment()
            val args = Bundle()
            args.putInt(MAP_ID, mapId)
            args.putSerializable(MARKER_ID, marker)
            f.arguments = args
            return f
        }
    }
}