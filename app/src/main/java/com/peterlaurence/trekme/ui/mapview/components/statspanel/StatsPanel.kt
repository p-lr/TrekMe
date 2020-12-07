package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayout
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDuration
import com.peterlaurence.trekme.core.units.UnitFormatter.formatElevation
import com.peterlaurence.trekme.databinding.StatsPanelLayoutBinding

class StatsPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0) : FlexboxLayout(context, attrs, defStyleAttr) {

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
        binding.statElevationUp.setText(formatElevation(elevationUp))
    }

    fun setElevationDown(elevationDown: Double) {
        binding.statElevationDown.setText(formatElevation(elevationDown))
    }

    fun setChrono(chrono: Long?) {
        binding.statChrono.setText(if (chrono != null) formatDuration(chrono) else "-")
    }
}