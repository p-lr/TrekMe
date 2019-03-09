package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.gson.Landmark

/**
 * This [android.widget.ImageView] has two states :
 * * Static : it appears as a classic "point of interest" landmark
 * * Dynamic : it has a round shape with arrows turning around it, indicating that it can be moved
 *
 * The transitions between the two states are animated, using animated vector drawables.
 *
 * @author peterLaurence on 04/03/19.
 */
class MovableLandmark
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val rounded = context.getDrawable(R.drawable.avd_landmark_rounded) as AnimatedVectorDrawable
    private val static = context.getDrawable(R.drawable.vd_landmark_location_rounded) as Drawable
    private val staticToDynamic = context.getDrawable(R.drawable.avd_landmark_location_rounded) as AnimatedVectorDrawable
    private val dynamicToStatic = context.getDrawable(R.drawable.avd_landmark_rounded_location) as AnimatedVectorDrawable
    private lateinit var landmark: Landmark
    private var isStatic = false

    /* The relative coordinates are kept here. Although this shouldn't be a concern of this object,
     * the TileView don't offer the possibility to retrieve the relative coordinates of a marker.
     * Saving them in the landmark's view is one of the possible workarounds.
     */
    var relativeX: Double? = null
    var relativeY: Double? = null

    /**
     * The [rounded] drawable is just the end state of the [staticToDynamic]
     * `AnimatedVectorDrawable`. <br>
     * The [static] drawable is the end state of the [dynamicToStatic]
     * `AnimatedVectorDrawable`. <br>
     */
    constructor(context: Context, staticForm: Boolean, landmark: Landmark) : this(context, null, 0) {
        this.landmark = landmark

        if (staticForm) {
            initStatic()
        } else {
            initRounded()
        }
    }

    fun initStatic() {
        isStatic = true
        setImageDrawable(static)
    }

    fun initRounded() {
        isStatic = false
        setImageDrawable(rounded)
        rounded.start()
    }

    fun morphToStaticForm() {
        if (!isStatic) {
            stopCurrentAnimation()
            setImageDrawable(dynamicToStatic)
            dynamicToStatic.start()
            isStatic = true
        }
    }

    fun morphToDynamicForm() {
        if (isStatic) {
            stopCurrentAnimation()
            setImageDrawable(staticToDynamic)
            staticToDynamic.start()
            isStatic = false
        }
    }

    private fun stopCurrentAnimation() {
        val drawable = drawable
        if (drawable is AnimatedVectorDrawable) {
            drawable.stop()
        }
    }

    /**
     * Get the reference on the [Landmark] this [MovableLandmark] refers to.
     */
    fun getLandmark(): Landmark {
        return landmark
    }
}