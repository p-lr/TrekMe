package com.peterlaurence.trekadvisor.menu.trackview.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekadvisor.R
import kotlinx.android.synthetic.main.track_view_elevation_stack.view.*

class TrackElevationStackView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.track_view_elevation_stack, this)
    }

    fun setElevationStack(elevationUpStackText: String, elevationDownStackText: String) {
        elevationUpText.text = elevationUpStackText
        elevationDownText.text = elevationDownStackText
        invalidate()
    }
}