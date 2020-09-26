package com.peterlaurence.trekme.ui.mapview.components.statspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.databinding.StatWithImageBinding

class StatChrono @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: StatWithImageBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = StatWithImageBinding.inflate(inflater, this)
        binding.image.background = ContextCompat.getDrawable(context, R.drawable.timer_16dp)

        /* Initial value */
        setText("00:00")
    }

    fun setText(txt: String) {
        binding.text.text = txt
    }
}