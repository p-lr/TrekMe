package com.peterlaurence.trekme.ui.wifip2p.widgets

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.peterlaurence.trekme.R

/**
 *
 */
class WaveSearchIndicator @JvmOverloads constructor(
        ctx: Context,
        attr: AttributeSet? = null,
        defStyleAttr: Int = 0): AppCompatImageButton(ctx, attr, defStyleAttr) {

    private val avd: AnimatedVectorDrawable = ctx.getDrawable(R.drawable.avd_wave_search) as AnimatedVectorDrawable
    init {
        setImageDrawable(avd)
    }

    fun start() = avd.start()
    fun stop() = if (avd.isRunning) avd.stop() else Unit
}