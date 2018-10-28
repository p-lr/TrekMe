package com.peterlaurence.trekadvisor.menu.trackview.components

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekadvisor.R
import kotlinx.android.synthetic.main.track_view_distance.view.*

class TrackDistanceView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.track_view_distance, this)
    }

    fun setDistanceText(dist: String) {
        distanceText.text = dist
        invalidate()
    }
}