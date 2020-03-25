package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.peterlaurence.mapview.MapView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.FragmentMapViewBinding
import com.peterlaurence.trekme.ui.mapview.components.CompassView
import com.peterlaurence.trekme.ui.mapview.components.PositionOrientationMarker

/**
 * Presenter for [MapViewFragment]. It is loosely coupled with it, so a different view could be used.
 * It uses data binding from Android Jetpack.
 *
 * @author peterLaurence on 19/03/16 -- Converted to Kotlin on 10/05/2019
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
        layout.addView(mapView, 0)

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

    interface PositionTouchListener {
        fun onPositionTouch()
    }
}
