package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.StatWithImageBinding

class StatElevationUp @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: StatWithImageBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = StatWithImageBinding.inflate(inflater, this )
        binding.image.background = context.getDrawable(R.drawable.elevation_up)

        setText("0 m")
    }

    fun setText(txt: String) {
        binding.text.text = txt
    }
}