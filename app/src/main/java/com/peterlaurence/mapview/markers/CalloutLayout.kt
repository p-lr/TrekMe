package com.peterlaurence.mapview.markers

import android.content.Context
import android.view.View
import com.peterlaurence.mapview.MapView

class CalloutLayout(context: Context): MarkerLayout(context)

/**
 * Add a callout to the the MapView.  The callout can be any View.
 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters.
 *
 * @param view    View instance to be added to the MapView.
 * @param x       Relative x position the View instance should be positioned at.
 * @param y       Relative y position the View instance should be positioned at.
 * @param relativeAnchorLeft The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
 * @param relativeAnchorTop  The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
 * @param absoluteAnchorLeft The x-axis position of a marker will be offset by this value.
 * @param absoluteAnchorTop  The y-axis position of a marker will be offset by this value.
 */
fun MapView.addCallout(view: View, x: Double, y: Double, relativeAnchorLeft: Float = -0.5f,
                      relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                      absoluteAnchorTop: Float = 0f) {
    calloutLayout.addMarker(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y),
            relativeAnchorLeft, relativeAnchorTop,
            absoluteAnchorLeft, absoluteAnchorTop
    )
}

/**
 * Removes a callout View from the MapView's view tree.
 *
 * @param view The callout View to be removed.
 */
fun MapView.removeCallout(view: View) {
    calloutLayout.removeMarker(view)
}