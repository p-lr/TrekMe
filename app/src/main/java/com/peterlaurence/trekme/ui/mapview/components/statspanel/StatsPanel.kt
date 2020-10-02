package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.peterlaurence.trekme.databinding.StatsPanelLayoutBinding
import com.peterlaurence.trekme.util.formatDistance
import com.peterlaurence.trekme.util.formatDuration

class StatsPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0): FlexboxLayout(context, attrs, defStyleAttr) {

    private val binding: StatsPanelLayoutBinding

    init {
        flexDirection = FlexDirection.ROW
        val inflater = LayoutInflater.from(context)
        binding = StatsPanelLayoutBinding.inflate(inflater, this)
    }

    fun setDistance(dist: Double) {
        binding.statDistance.setText(formatDistance(dist))
    }

    fun setElevationUp(elevationUp: Double) {
        binding.statElevationUp.setText(formatDistance(elevationUp))
    }

    fun setElevationDown(elevationDown: Double) {
        binding.statElevationDown.setText(formatDistance(elevationDown))
    }

    fun setChrono(chrono: Long?) {
        binding.statChrono.setText(if (chrono != null) formatDuration(chrono) else "-")
    }
}