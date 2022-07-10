package com.peterlaurence.trekme.features.wifip2p.presentation.ui.widgets

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import com.peterlaurence.trekme.R

/**
 * An indicator which animates a wave (like a sonar).
 * It nicely represents a discovery action.
 *
 * @author P.Laurence on 21/04/20
 */
class WaveSearchIndicator @JvmOverloads constructor(
        ctx: Context,
        attr: AttributeSet? = null,
        defStyleAttr: Int = 0): AppCompatImageButton(ctx, attr, defStyleAttr) {

    private val avd: AnimatedVectorDrawable = ctx.getDrawable(R.drawable.avd_wave_search) as AnimatedVectorDrawable
    init {
        setImageDrawable(avd)
    }

    fun start() = if (!avd.isRunning) avd.start() else Unit
    fun stop() = if (avd.isRunning) avd.stop() else Unit
}