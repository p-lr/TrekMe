package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.mapview.components.PositionOrientationMarker
import kotlinx.android.synthetic.main.fragment_map_view.view.*

/**
 * Layout for [MapViewFragment]. It is loosely coupled with it, so a different layout could be used.
 *
 * @author peterLaurence on 19/03/16 -- Converted to Kotlin on 10/05/2019
 */
class FrameLayoutMapView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    lateinit var positionMarker: View
        private set

    private var positionTouchListener: PositionTouchListener? = null

    val speedIndicator: MapViewFragment.SpeedListener
        get() = indicator_overlay

    val distanceIndicator: DistanceLayer.DistanceListener
        get() = indicator_overlay

    init {
        init(context)
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.fragment_map_view, this)

        fab_position.setOnClickListener {
            /* How to change the icon color */
            fab_position.drawable.mutate().setTint(resources.getColor(R.color.colorAccent, null))
            positionTouchListener?.onPositionTouch()
        }

        positionMarker = PositionOrientationMarker(context)
    }

    fun setPositionTouchListener(listener: PositionTouchListener) {
        positionTouchListener = listener
    }

    interface PositionTouchListener {
        fun onPositionTouch()
    }
}
