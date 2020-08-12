package com.peterlaurence.trekme.ui.mapcreate.components

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.peterlaurence.trekme.R

/**
 * Custom marker at the center of an area, used to move to whole area.
 *
 * @author P.Laurence on 12/08/20
 */
class AreaMarkerCentral
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    init {
        setImageResource(R.drawable.area_move_marker)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}