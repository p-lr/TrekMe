package com.peterlaurence.trekme.ui.common

import android.animation.TimeInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A call-out which animates its entry with an overshoot scaling interpolator.
 *
 * @param shouldAnimate Controls whether there should be an entering animation or not.
 * @param delayMs Delay of the entering animation.
 * @param popupOrigin Defines from where the callout will popup (only applicable if [shouldAnimate]
 * is true).
 * @param onAnimationDone Callback invoked when entering animation is done. The parent can use this
 * to change the [shouldAnimate] state.
 */
@Composable
fun Callout(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(5.dp),
    shouldAnimate: Boolean = true,
    delayMs: Long = 0,
    popupOrigin: PopupOrigin = PopupOrigin.BottomCenter,
    onAnimationDone: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var animVal by remember { mutableStateOf(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(true) {
        if (shouldAnimate) {
            delay(delayMs)
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = tween(250, easing = overshootEasing)
            ) {
                animVal = value
            }
            onAnimationDone()
        }
    }
    Surface(
        modifier
            .alpha(animVal)
            .graphicsLayer {
                scaleX = animVal
                scaleY = animVal
                transformOrigin = getTransformOrigin(popupOrigin)
            },
        shape = shape,
        elevation = 10.dp,
        content = content
    )
}

enum class PopupOrigin {
    TopStart, TopEnd, TopCenter, BottomStart, BottomEnd, BottomCenter
}

private fun getTransformOrigin(popupOrigin: PopupOrigin): TransformOrigin {
    return when (popupOrigin) {
        PopupOrigin.TopStart -> TransformOrigin(0f, 0f)
        PopupOrigin.TopEnd -> TransformOrigin(1f, 0f)
        PopupOrigin.TopCenter -> TransformOrigin(0.5f, 1f)
        PopupOrigin.BottomStart -> TransformOrigin(0f, 1f)
        PopupOrigin.BottomEnd -> TransformOrigin(1f, 1f)
        PopupOrigin.BottomCenter -> TransformOrigin(0.5f, 1f)
    }
}

private val overshootEasing = OvershootInterpolator(1.2f).toEasing()

private fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}