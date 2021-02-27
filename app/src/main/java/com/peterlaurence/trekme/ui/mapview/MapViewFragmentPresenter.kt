package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.track.TrackStatistics
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.databinding.FragmentMapViewBinding
import com.peterlaurence.trekme.repositories.location.Location
import com.peterlaurence.trekme.ui.mapview.components.CompassView
import com.peterlaurence.trekme.ui.mapview.components.PositionOrientationMarker
import ovh.plrapps.mapview.MapView

/**
 * Presenter for [MapViewFragment]. It is loosely coupled with it, so a different view could be used.
 * It uses data binding from Android Jetpack.
 *
 * @author P.Laurence on 19/03/16 -- Converted to Kotlin on 10/05/2019
 */
class MapViewFragmentPresenter
constructor(layoutInflater: LayoutInflater, container: ViewGroup?, context: Context) : MapViewFragmentContract.Presenter {
    private val binding = FragmentMapViewBinding.inflate(layoutInflater, container, false)
    private val layout = binding.root as ConstraintLayout

    private val customView: MapViewFragmentContract.View = object : MapViewFragmentContract.View {
        private val marker = PositionOrientationMarker(context)

        override val speedIndicator: MapViewFragment.SpeedListener
            get() = binding.indicatorOverlay
        override val distanceIndicator: DistanceLayer.DistanceListener
            get() = binding.indicatorOverlay
        override val positionMarker: PositionOrientationMarker
            get() = marker
        override val compassView: CompassView
            get() = binding.compass
    }

    override val androidView: View
        get() = binding.root

    override val view: MapViewFragmentContract.View
        get() = customView

    private var positionTouchListener: PositionTouchListener? = null
    private var lastLocation: Location? = null

    init {
        binding.fabPosition.setOnClickListener {
            /* How to change the icon color */
            binding.fabPosition.drawable.mutate().setTint(context.resources.getColor(R.color.colorAccent, null))
            positionTouchListener?.onPositionTouch()
        }
    }

    override fun setPositionTouchListener(listener: PositionTouchListener) {
        positionTouchListener = listener
    }

    override fun setMapView(mapView: MapView) {
        /* The MapView should take all space available horizontally, and fill available space
         * vertically*/
        mapView.layoutParams = ViewGroup.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 0)

        /* This method should be invoked only once during the lifecycle of this presenter.
         * But just in case, replace the existing MapView */
        val previous = layout.children.firstOrNull() as? MapView
        if (previous != null) {
            layout.removeView(previous)
        }
        layout.addView(mapView, 0)

        /* We ensure the the bottom of the MapView is connected to the top of the stats panel, so
         * they don't overlap */
        val constraintSet = ConstraintSet()
        constraintSet.clone(layout)
        constraintSet.connect(mapView.id, ConstraintSet.TOP, binding.mapViewFrgmtLayout.id, ConstraintSet.TOP)
        constraintSet.connect(mapView.id, ConstraintSet.BOTTOM, binding.statsPanel.id, ConstraintSet.TOP)
        constraintSet.applyTo(layout)

        binding.fabPosition.visibility = View.VISIBLE
        binding.frgmtMapViewMsg.visibility = View.GONE
    }

    override fun removeMapView(mapView: MapView?) {
        if (mapView != null) {
            layout.removeView(mapView)
        }

        binding.indicatorOverlay.visibility = View.GONE
        binding.fabPosition.visibility = View.GONE
    }

    override fun showMessage(msg: String) {
        binding.message = msg
        binding.frgmtMapViewMsg.visibility = View.VISIBLE
    }

    override fun showStatistics(trackStatistics: TrackStatistics) {
        binding.statsPanel.visibility = View.VISIBLE
        binding.statsPanel.setDistance(trackStatistics.distance)
        binding.statsPanel.setElevationUp(trackStatistics.elevationUpStack)
        binding.statsPanel.setElevationDown(trackStatistics.elevationDownStack)
        binding.statsPanel.setChrono(trackStatistics.durationInSecond)
    }

    override fun hideStatistics() {
        binding.statsPanel.visibility = View.GONE
    }

    override fun setGpsData(location: Location) {
        lastLocation = location
        if (binding.gpsDataPanel.visibility == View.VISIBLE) {
            updateGpsDataPanel(location)
        }
    }

    override fun setGpsDataVisible(visible: Boolean) {
        binding.gpsDataPanel.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            val loc = lastLocation ?: return
            updateGpsDataPanel(loc)
        }
    }

    private fun updateGpsDataPanel(location: Location) {
        binding.latValue.text = UnitFormatter.formatLatLon(location.latitude)
        binding.lonValue.text = UnitFormatter.formatLatLon(location.longitude)
        binding.eleValue.text = UnitFormatter.formatElevation(location.altitude)
    }

    interface PositionTouchListener {
        fun onPositionTouch()
    }
}
