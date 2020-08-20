package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.view.View
import android.view.animation.*
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import com.peterlaurence.trekme.R
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * This callout is shown when a landmark is taped.
 *
 * @author peterLaurence on 09/03/19.
 */
class LandmarkCallout(context: Context) : RelativeLayout(context) {
    private val moveButton: ImageButton
    private val deleteButton: ImageButton
    private val title: TextView
    private val subTitle: TextView

    init {

        View.inflate(context, R.layout.landmark_callout, this)

        moveButton = findViewById(R.id.move_callout_btn)
        deleteButton = findViewById(R.id.delete_callout_btn)
        subTitle = findViewById(R.id.callout_subtitle)
        title = findViewById(R.id.callout_title)
        title.text = context.getString(R.string.callout_landmark_title)

        moveButton.drawable.setTint(getContext().getColor(R.color.colorAccent))
        deleteButton.drawable.setTint(getContext().getColor(R.color.colorAccent))
    }

    fun transitionIn() {
        val scaleAnimation = ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 1f)
        scaleAnimation.interpolator = OvershootInterpolator(1.2f)
        scaleAnimation.duration = 250

        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.duration = 200

        val animationSet = AnimationSet(false)

        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)

        startAnimation(animationSet)
    }

    fun setMoveAction(moveAction: () -> Unit) {
        moveButton.setOnClickListener { moveAction() }
    }

    fun setDeleteAction(deleteAction: () -> Unit) {
        deleteButton.setOnClickListener { deleteAction() }
    }

    private fun setSubTitle(subtitle: String) {
        subTitle.text = subtitle
    }

    fun setSubTitle(lat: Double, lon: Double) {
        val df = DecimalFormat("#.####")
        df.roundingMode = RoundingMode.CEILING

        /* Note the the compiler uses StringBuilder under the hood */
        setSubTitle("lat : " + df.format(lat) + "  " + "lon : " + df.format(lon))
    }
}
